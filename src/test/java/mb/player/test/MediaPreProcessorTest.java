package mb.player.test;

import mb.player.media.MPMedia;
import mb.player.media.MediaPreProcessor;
import static org.junit.Assert.*;
import org.junit.Test;

public class MediaPreProcessorTest {
    
    @Test
    public void testMp3MetadataDiscoveredCorrectly() throws Exception {
        
        // Prepare
        MPMedia media = new MPMedia("click-track.mp3", 
                TestUtils.loadDefaultAudioFile().toURI().toString(), MPMedia.Type.AUDIO);
        
        // Execute
        MediaPreProcessor processor = new MediaPreProcessor(media);
        
        // Test
        assertTrue(processor.getTotalSamples() > 0);
        assertTrue(processor.getSampleRate() > 0);
        assertTrue(processor.getDurationSec() > 0);
    }
    
    @Test
    public void testFlac16BitMetadataDiscoveredCorrectly() throws Exception {
        
        // Prepare
        MPMedia media = new MPMedia("click-track-16bit.flac", 
                TestUtils.loadAudioFile("/click-track-16bit.flac").toURI().toString(), 
                MPMedia.Type.AUDIO);
        
        // Execute
        MediaPreProcessor processor = new MediaPreProcessor(media);
        
        // Test
        assertTrue(processor.getTotalSamples() > 0);
        assertTrue(processor.getSampleRate() > 0);
        assertTrue(processor.getDurationSec() > 0);
    }
    
    @Test
    public void testFlac24BitMetadataDiscoveredCorrectly() throws Exception {
        
        // Prepare
        MPMedia media = new MPMedia("click-track-24bit.flac", 
                TestUtils.loadAudioFile("/click-track-24bit.flac").toURI().toString(), 
                MPMedia.Type.AUDIO);
        
        // Execute
        MediaPreProcessor processor = new MediaPreProcessor(media);
        
        // Test
        assertTrue(processor.getTotalSamples() > 0);
        assertTrue(processor.getSampleRate() > 0);
        assertTrue(processor.getDurationSec() > 0);
    }
}
