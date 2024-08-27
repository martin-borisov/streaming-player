package mb.player.media;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import mb.jflac.FLACDecoder;
import mb.jflac.metadata.StreamInfo;

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
            }
        } catch (IOException e) {
            LOG.log(Level.FINE, "Fetching artwork of media ''{0}'' failed", media);
            LOG.log(Level.FINE, e.getMessage(), e);
        }
        
        // Fetch attributes
        if(media.isLocal()) {
            File audioFile = new File(URI.create(media.getSource()));
            
            if(media.getSource().endsWith("flac")) {
                
                try(FileInputStream fis = new FileInputStream(audioFile)) {
                    
                    FLACDecoder decoder = new FLACDecoder(fis);
                    decoder.decode();
                    StreamInfo info = decoder.getStreamInfo();
                    if(info != null) {
                        totalSamples = info.getTotalSamples();
                        sampleRate = info.getSampleRate();
                        
                        if(totalSamples > 0 && sampleRate > 0) {
                            durationSec = (long) (totalSamples / sampleRate);
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "FLAC pre processing failed", e);
                } 
                
            } else if(media.getSource().endsWith("mp3")) {
                
                try {
                    Mp3File mp3file = new Mp3File(audioFile);
                    durationSec = mp3file.getLengthInSeconds();
                    sampleRate = mp3file.getSampleRate();
                    totalSamples = sampleRate * durationSec;
                    
                    if(mp3file.hasId3v2Tag()) {
                        ID3v2 id3v2Tag = mp3file.getId3v2Tag();
                        attributes.put("title", id3v2Tag.getTitle());
                        attributes.put("artist", id3v2Tag.getArtist());
                        attributes.put("album", id3v2Tag.getAlbum());
                        
                        // TODO Fetch more attributes
                        
                        // Fetch album art
                        if(attributes.get("artwork") == null) {
                            byte[] bytes = id3v2Tag.getAlbumImage();
                            if(bytes != null) {
                                attributes.put("artwork", 
                                        MPUtils.imageFromID3TagSwing(new ByteArrayInputStream(bytes)));
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "MP3 pre processing failed", e);
                }
                
            } else if(media.getSource().endsWith("wav")) {
                // TODO
            }
        }
    }

}
