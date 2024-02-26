package mb.player.media.audio;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import mb.jflac.sound.spi.FlacAudioFileReader;

public class AudioSystemWrapper {
    private static final Logger LOG = Logger.getLogger(AudioSystemWrapper.class.getName());
    
    /**
     * Used for unit testing
     */
    private static SourceDataLine defaultSourceDataLine;

    public static AudioFileFormat getAudioFileFormat(final AudioSource source)
            throws UnsupportedAudioFileException, IOException, AudioPlayerException {
        
        AudioFileFormat format;
        if(source.isFile()) {
            try {
                
                // Fix for provider ordering and MPEG provider consuming FLAC streams
                format = new FlacAudioFileReader().getAudioFileFormat(source.getFile());
            } catch (Exception e) {
                format = AudioSystem.getAudioFileFormat(source.getFile());
            }
        } else {
            try {
                
                // Fix for provider ordering and MPEG provider consuming FLAC streams
                format = new FlacAudioFileReader().getAudioFileFormat(source.openStream());
            } catch (Exception e) {
                format = AudioSystem.getAudioFileFormat(source.openStream());
            }
        }
        
        LOG.log(Level.FINE, "Audio format for source ''{0}'' is ''{1}''", 
                new Object[] {source, format.getClass().getName()});
        
        return format;
    }
    
    public static AudioInputStream getAudioInputStream(final AudioSource source)
            throws UnsupportedAudioFileException, IOException, AudioPlayerException {
        
        AudioInputStream stream;
        if(source.isFile()) {
            try {
                
                // Fix for provider ordering and MPEG provider consuming FLAC streams
                stream = new FlacAudioFileReader().getAudioInputStream(source.getFile());
            } catch (Exception e) {
                stream = AudioSystem.getAudioInputStream(source.getFile());
            }
        } else {
            try {
                
                // Fix for provider ordering and MPEG provider consuming FLAC streams
                stream = new FlacAudioFileReader().getAudioInputStream(source.openStream());
            } catch (Exception e) {
                stream = AudioSystem.getAudioInputStream(source.openStream());
            }
        }
        
        LOG.log(Level.FINE, "Audio input stream for source ''{0}'' is ''{1}''", 
                new Object[] {source, stream.getClass().getName()});
        
        return stream;
    }
    
    /**
     * Used for unit testing
     */
    public static SourceDataLine getSourceDataLine(AudioFormat format) throws LineUnavailableException {
        return defaultSourceDataLine != null ? 
                defaultSourceDataLine : AudioSystem.getSourceDataLine(format);
    }
    
    /**
     * Used for unit testing
     */
    public static void setDefaultSourceDataLine(SourceDataLine line) {
        defaultSourceDataLine = line;
    }
}
