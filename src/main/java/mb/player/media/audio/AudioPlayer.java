package mb.player.media.audio;

import static java.text.MessageFormat.format;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioPlayer {
    private static final Logger LOG = Logger.getLogger(AudioPlayer.class.getName());
    
    private static final int BUFFER_BYTES_PER_FRAME_BYTE = 1024;
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    
    private AudioSource source;
    private AudioFileFormat sourceFormat;
    private AudioFormat targetFormat;
    private AudioInputStream encodedIn, decodedIn;
    private SourceDataLine out;
    private volatile boolean keepRunning;
    private List<AudioPlayerListener> listeners;
    private Accumulator totalBytesSincePlayStarted;
    
    public AudioPlayer() {
        listeners = new ArrayList<>();
        totalBytesSincePlayStarted = new Accumulator();
    }
    
    /* Public */
    
    public void open(File file) throws AudioPlayerException {
        open(new AudioSource(file));
    }
    
    public void open(URL url) throws AudioPlayerException {
        open(new AudioSource(url));
    }
    
    public void open(AudioSource source) throws AudioPlayerException {
        this.source = source;
        
        fine("Cleaning up player before opening new media");
        stop();
        try {
            fine("Getting file format for audio in");
            sourceFormat = getAudioFileFormat(source);
            fine("Audio in format: " + sourceFormat);
            
            fine("Building file format for audio out");
            targetFormat = buildAudioOutFormat(sourceFormat);
            fine("Audio out format: " + targetFormat);
            
            fine("Setting up streams");
            setupStreams(source);
            
            fine("Getting data line for audio out");
            out = getSourceDataLine(targetFormat);
            fine("Data line successfully returned by system: " + out.getFormat());
            
            fine("Adding line listener which handles player events");
            out.addLineListener(event -> postAudioPlayerEventFromLineEvent(event));
            
        } catch (Exception e) {
            fine("There was an error opening media so cleaning up");
            stop();
            throw new AudioPlayerException("Failed to open media", e);
        }
    }
    
    public void play() throws AudioPlayerException {
        if(out != null && !out.isActive()) {
            
            // Open line
            if (!out.isOpen()) {
                LOG.fine("Trying to open source data line");
                try {
                    out.open();
                } catch (LineUnavailableException e) {
                    throw new AudioPlayerException(e);
                }
                LOG.fine("Source data line successfully opened");
            }
            
            // Start line
            out.start();
            
            // Start playback
            totalBytesSincePlayStarted.reset();
            startAudioPlaybackThread();
        } else {
            throw new AudioPlayerException(
                    "Player not initialized -> call open(...) before play() "
                    + "or line is currently active");
        }
    }
    
    public void pause() throws AudioPlayerException {
        verifyInitializedAndOpen();
        if (isPlaying()) {
            keepRunning = false;
            listeners.forEach(l -> l.onPause());
        }
    }
    
    public void resume() throws AudioPlayerException {
        verifyInitializedAndOpen();
        if(isPaused()) {
            startAudioPlaybackThread();
            listeners.forEach(l -> l.onResume());
        }
    }
    
    public void stop() {
        
        // Stop playback
        keepRunning = false;
        
        // Invalidate line
        Optional.ofNullable(out).ifPresent(out -> {
            out.drain();
            out.close();
        });
        out = null;
        
        // Invalidate input streams
        closeAudioInputStream(encodedIn);
        encodedIn = null;
        closeAudioInputStream(decodedIn);
        decodedIn = null;
        
        // Invalidate formats
        sourceFormat = null;
        targetFormat = null;
        
        // Notify listeners
        listeners.forEach(l -> l.onStop());
    }
    
    public void seekTo(int sec) throws AudioPlayerException {
        // TODO Validate requested seek length based on track length
        
        verifyInitializedAndOpen();
        
        // Calculate the byte index to which to skip from the provided second
        final int bytesPerSecond = (int) (targetFormat.getSampleRate() * targetFormat.getFrameSize());
        final int numBytesToSkip = bytesPerSecond * sec;
        fine(format("Trying to seek to sec {0} and byte index {1,number,#}", sec, numBytesToSkip));
        
        // Prepare audio resources
        prepareResourcesForSeek();
        
        // Read all bytes until the number of bytes are skipped without writing to output data line
        int bytesRead = 0;
        byte[] buffer = new byte[calcBufferSize()];
        Accumulator totalBytesRead = new Accumulator();
        
        if(sec > 0) {
            try {
                while (totalBytesRead.getValue() < numBytesToSkip
                        && (bytesRead = decodedIn.read(buffer, 0, buffer.length)) != -1) {
                    totalBytesRead.add(bytesRead);
                }
            } catch (IOException e) {
                fine("Reading bytes failed", e);
            }
        }
        
        // Update elapsed time
        final int elapsedSeconds = (int) (totalBytesRead.getValue() / bytesPerSecond);
        fine(format("Seeked to {0,number,#} bytes and {1} seconds", totalBytesRead.getValue(), elapsedSeconds));
                    
        // Notify listeners of progress asynchronously
        if (elapsedSeconds > 0) {
            new Thread(() -> {
                synchronized (listeners) {
                    listeners.forEach(l -> l.onProgress(elapsedSeconds));
                }
            }).start();
        }
        
        // Play
        totalBytesSincePlayStarted.reset();
        totalBytesSincePlayStarted.add(totalBytesRead.getValue());
        resumePlayAfterSeek();
        
        // TODO Notify listeners for seek completion
    }
    
    public boolean isPlaying() {
        return out != null && out.isActive() && keepRunning;
    }
    
    public boolean isPaused() {
        return out != null && out.isActive() && !keepRunning;
    }
    
    public void addListener(AudioPlayerListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(AudioPlayerListener listener) {
        listeners.remove(listener);
    }
    
    public float getMaxVolume() {
        float value = 0;
        if(out != null) {
            FloatControl ctrl = getMasterGainControl();
            if(ctrl != null) {
                value = ctrl.getMaximum();
            }
        }
        return value;
    }
    
    public float getMinVolume() {
        float value = 0;
        if(out != null) {
            FloatControl ctrl = getMasterGainControl();
            if(ctrl != null) {
                value = ctrl.getMinimum();
            }
        }
        return value;
    }
    
    public float getVolume() {
        float value = 0;
        if (out != null) {
            FloatControl ctrl = getMasterGainControl();
            if(ctrl != null) {
                value = ctrl.getValue();
            }
        }
        return value;
    }
    
    public void setVolume(float value) {
        if(out != null) {
            FloatControl ctrl = getMasterGainControl();
            if(ctrl != null) {
                ctrl.setValue(value);
            }
        }
    }
    
    public boolean isSeekSupported() {
        return decodedIn != null && source.isFile();
    }
    
    /* Private */
    
    private void setupStreams(AudioSource source) throws AudioPlayerException {
        fine("Getting audio input stream for audio in");
        encodedIn = getAudioInputStream(source);
        decodedIn = getDecodedAudioInputStream(encodedIn, targetFormat);
        fine("Audio input stream successfully returned by system");
    }
    
    private void startAudioPlaybackThread() {
        byte[] buffer = new byte[calcBufferSize()];
        fine("Starting playback with buffer size: " + buffer.length);
        
        keepRunning = true;
        new Thread(() -> {
            
            // Do some preparation for progress event handling
            ExecutorService executor = Executors.newSingleThreadExecutor();
            final int bytesPerSecond = (int) (targetFormat.getSampleRate() * targetFormat.getFrameSize());
            Accumulator bytesWritten = new Accumulator();
            
            int bytesRead = -1;
            try {
                while (keepRunning && (bytesRead = decodedIn.read(buffer, 0, buffer.length)) != -1) {
                    
                    // Write data to data line
                    int writtenNow = out.write(buffer, 0, bytesRead);
                    bytesWritten.add(writtenNow);
                    totalBytesSincePlayStarted.add(writtenNow);
                    final int elapsedSeconds = (int) (totalBytesSincePlayStarted.getValue() / bytesPerSecond);
                    
                    // Notify listeners of progress asynchronously
                    if(bytesWritten.getValue() >= bytesPerSecond) {
                        executor.execute(() -> {
                            synchronized (listeners) {
                                listeners.forEach(l -> l.onProgress(elapsedSeconds));
                            }
                        });
                        bytesWritten.reset();
                    }
                }
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Audio playback failed", e);
            }
            
            // Check if stream ended or externally paused
            if(keepRunning) {
                fine("End of stream reached; playback thread completing");
                keepRunning = false;
                postEndOfMediaEvent();
            } else {
                fine("Stream paused; playback thread completing");
            }
            executor.shutdown();
            
        }).start();
    }
    
    private int calcBufferSize() {
        int bufSize = DEFAULT_BUFFER_SIZE;
        int frameSize = targetFormat.getFrameSize();
        
        // NB: This applies only to PCM encoding
        bufSize = (frameSize != AudioSystem.NOT_SPECIFIED ? frameSize * BUFFER_BYTES_PER_FRAME_BYTE : bufSize);
        return bufSize;
    }
    
    private void postAudioPlayerEventFromLineEvent(LineEvent event) {
        fine("Event received from audio data line: " + event.getType().toString());
        String type = event. getType().toString();
        if(type.equals(LineEvent.Type.OPEN.toString())) {
            Map<String, Object> props = new HashMap<String, Object>(sourceFormat.properties());
            props.putAll(sourceFormat.getFormat().properties());
            props.put("audio.type", sourceFormat.getType());
            props.put("audio.samplerate.hz", sourceFormat.getFormat().getSampleRate());
            props.put("audio.samplesize.bits", sourceFormat.getFormat().getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED ? 
                    targetFormat.getSampleSizeInBits() : sourceFormat.getFormat().getSampleSizeInBits());
            props.put("audio.channels", sourceFormat.getFormat().getChannels());
            listeners.forEach(l -> l.onOpen(props));
        } else if(type.equals(LineEvent.Type.START.toString())) {
            listeners.forEach(l -> l.onStart());
        } else if(type.equals(LineEvent.Type.STOP.toString())) {
            listeners.forEach(l -> l.onStop());
        } else if(type.equals(LineEvent.Type.CLOSE.toString())) {
            listeners.forEach(l -> l.onClose());
        }
    }
    
    private void postEndOfMediaEvent() {
        listeners.forEach(l -> l.onEndOfMedia());
    }
    
    private void verifyInitializedAndOpen() throws AudioPlayerException {
        if(out == null || !out.isOpen()) {
            throw new AudioPlayerException("Player not initialized; call open(...) first");
        }
    }
    
    
    private FloatControl getMasterGainControl() {
        FloatControl ctrl = null;
        try {
            ctrl = (FloatControl) out.getControl(FloatControl.Type.MASTER_GAIN);
        } catch (IllegalArgumentException e) {
            fine(format("Master Gain control not available on source data line: {0}", out.getLineInfo()), e);
        }
        return ctrl;
    }
    
    private void prepareResourcesForSeek() {
        
        // Stop playback
        keepRunning = false;
        
        // Invalidate input streams
        closeAudioInputStream(encodedIn);
        encodedIn = null;
        closeAudioInputStream(decodedIn);
        decodedIn = null;
        
        try {
            setupStreams(source);
        } catch (AudioPlayerException e) {
            fine("Streams reset for seek failed", e);
        }
    }
    
    private void resumePlayAfterSeek() throws AudioPlayerException {
        startAudioPlaybackThread();
    }
    
    private void fine(String msg) {
        LOG.fine(msg);
    }
    
    private void fine(String msg, Throwable e) {
        LOG.log(Level.FINE, msg, e);
    }
    
    /* Utils */
    
    private static AudioFormat buildAudioOutFormat(AudioFileFormat inFormat) {
        AudioFormat format = inFormat.getFormat();
        return new AudioFormat(
                Encoding.PCM_SIGNED, 
                getDefaultIfNotSpecified(format.getSampleRate(), 44100), 
                getDefaultIfNotSpecified(format.getSampleSizeInBits(), 16), 
                getDefaultIfNotSpecified(format.getChannels(), 2), 
                getDefaultIfNotSpecified(format.getSampleSizeInBits(), 16) / 8 * getDefaultIfNotSpecified(format.getChannels(), 2), 
                getDefaultIfNotSpecified(format.getSampleRate(), 44100), 
                false);
    }
    
    private static AudioFileFormat getAudioFileFormat(AudioSource source) throws AudioPlayerException {
        try {
            return AudioSystemWrapper.getAudioFileFormat(source);
        } catch (Exception e) {
            throw new AudioPlayerException("Getting file format for input stream failed", e);
        } 
    }
    
    private static AudioInputStream getAudioInputStream(AudioSource source) throws AudioPlayerException {
        try {
            return AudioSystemWrapper.getAudioInputStream(source);
        } catch (UnsupportedAudioFileException e) {
            throw new AudioPlayerException("Audio file format not supported", e);
        } catch (IOException e) {
            throw new AudioPlayerException("Processing input stream failed", e);
        }
    }
    
    private static AudioInputStream getDecodedAudioInputStream(AudioInputStream ais, AudioFormat format) {
        return AudioSystem.getAudioInputStream(format, ais);
    }
    
    private static SourceDataLine getSourceDataLine(AudioFormat format) throws AudioPlayerException {
        try {
            return AudioSystemWrapper.getSourceDataLine(format);
        } catch (LineUnavailableException e) {
            throw new AudioPlayerException(e);
        }
    }
    
    private static int getDefaultIfNotSpecified(int value, int def) {
        return value == AudioSystem.NOT_SPECIFIED ? def : value;
    }
    
    private static float getDefaultIfNotSpecified(float value, float def) {
        return value == AudioSystem.NOT_SPECIFIED ? def : value;
    }
    
    private static void closeAudioInputStream(AudioInputStream ais) {
        Optional.ofNullable(ais).ifPresent(in -> {
            try {
                in.close();
            } catch (IOException e) {
                LOG.log(Level.FINE, "Closing audio input stream failed", e);
            }
        });
    }
}
