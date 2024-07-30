package mb.player.test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import static org.junit.Assert.assertNotNull;

public class TestUtils {
    
    public static File loadAudioFile(String path) throws URISyntaxException {
        URL resource = AudioPlayerTest.class.getResource(path);
        assertNotNull("Test file not found", resource);
        return new File(resource.toURI());
    }
    
    public static File loadDefaultAudioFile() throws URISyntaxException {
        return loadAudioFile("/click-track.mp3");
    }
}
