package mb.player.media;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFileFormat;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

import mb.jflac.metadata.StreamInfo;
import mb.jflac.sound.spi.FlacAudioFileReader;

public class MediaPreProcessor {
    
    private static final Logger LOG = Logger.getLogger(MediaPreProcessor.class.getName());
    private MPMedia media;
    private long totalSamples, sampleRate, durationSec;
    private Map<String, Object> attributes;

    public MediaPreProcessor(MPMedia media) {
        attributes = new HashMap<>();
        this.media = media;
        process();
    }
    
    public long getTotalSamples() {
        return totalSamples;
    }
    
    public long getSampleRate() {
        return sampleRate;
    }
    
    public long getDurationSec() {
        return durationSec;
    }
    
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    private void process() {
        
        // Fetch artwork
        try {
            BufferedImage image = MPUtils.fetchMediaCoverArtSwing(media);
            if(image != null) {
                attributes.put("artwork", image);
                LOG.log(Level.FINE, "Successfully fetched artwork of media ''{0}''", media);
            }
        } catch (IOException e) {
            LOG.log(Level.FINE, "Fetching artwork of media ''{0}'' failed", media);
            LOG.log(Level.FINE, e.getMessage(), e);
        }
        
        // Fetch attributes
        if(media.getSource().endsWith("flac")) {
            processFlac(media);
        } else if(media.getSource().endsWith("mp3")) {
            processMp3(media);
        } else if(media.getSource().endsWith("wav")) {
            // TODO Support for WAV metadata
        }
        
        LOG.log(Level.FINE, () -> {
            StringBuilder buf = new StringBuilder();
            buf.append(MessageFormat.format("Found {0} attributes of media ''{1}''", 
                    attributes.size(), media));
            attributes.forEach((k, v) -> {
                buf.append("\n - <").append(v.getClass().getSimpleName()).append("> ").append(k).append(" = ").append(v);
            });
            return buf.toString();
        });
    }
    
    private void processMp3(MPMedia media) {
        if(media.isLocal()) {
            File audioFile = new File(URI.create(media.getSource()));
            try {
                Mp3File mp3file = new Mp3File(audioFile);
                durationSec = mp3file.getLengthInSeconds();
                sampleRate = mp3file.getSampleRate();
                totalSamples = sampleRate * durationSec;

                if (mp3file.hasId3v2Tag()) {
                    ID3v2 id3v2Tag = mp3file.getId3v2Tag();
                    attributes.put("title", id3v2Tag.getTitle());
                    attributes.put("artist", id3v2Tag.getArtist());
                    attributes.put("album", id3v2Tag.getAlbum());

                    // TODO Fetch more mp3 attributes

                    // Fetch album art if not already available as cover file
                    if (attributes.get("artwork") == null) {
                        byte[] bytes = id3v2Tag.getAlbumImage();
                        if (bytes != null) {
                            attributes.put("artwork", MPUtils.imageFromID3TagSwing(
                                    new ByteArrayInputStream(bytes)));
                        }
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "MP3 pre processing failed", e);
            }
        } else {
            // TODO Support for remote mp3 files
        }
    }
    
    private void processFlac(MPMedia media) {
        FlacAudioFileReader reader = new FlacAudioFileReader();
        try {
            
            AudioFileFormat format;
            if(media.isLocal()) {
                
                File audioFile = new File(URI.create(media.getSource()));
                format = reader.getAudioFileFormat(audioFile);
            } else {
                
                URL url = URI.create(media.getSource()).toURL();
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setAuthenticator(MPUtils.createAuthenticator(media));
                if(con.getResponseCode() != 200) {
                    LOG.log(Level.WARNING, 
                            "FLAC pre processing failed; received HTTP response code: {0}", 
                            con.getResponseCode());
                    return;
                }
                
                format = reader.getAudioFileFormat(con.getInputStream());
            }

            // Getting the total sample count requires a quick hack unfortunately
            Field streamInfoField = reader.getClass().getDeclaredField("streamInfo");
            streamInfoField.setAccessible(true);
            StreamInfo info = (StreamInfo) streamInfoField.get(reader);
            if (info != null) {
                totalSamples = info.getTotalSamples();
                sampleRate = info.getSampleRate();

                if (totalSamples > 0 && sampleRate > 0) {
                    durationSec = (long) (totalSamples / sampleRate);
                }
            }

            // Fetch properties
            attributes.putAll(format.properties());

        } catch (Exception e) {
            LOG.log(Level.WARNING, "FLAC pre processing failed", e);
        }
    }

}
