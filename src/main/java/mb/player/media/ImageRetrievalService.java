package mb.player.media;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.text.MessageFormat.format;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class ImageRetrievalService {
    private static final Logger LOG = 
            Logger.getLogger(ImageRetrievalService.class.getName());
    private static ImageRetrievalService ref;

    public static ImageRetrievalService getInstance() {
        synchronized (ImageRetrievalService.class) {
            if(ref == null) {
                ref = new ImageRetrievalService();
            }
        }
        return ref;
    }
    
    private ImageRetrievalService() {
    }
    
    // TODO Implement image caching
    public BufferedImage fetchImage(URI uri, Authenticator auth) {
        LOG.log(Level.FINE, "Trying to fetch cover image at: ''{0}''", uri);
        
        BufferedImage image = null;
        if(isLocal(uri)) {
            image = fetchLocalImage(uri);
        } else if(isHttp(uri)){
            image = fetchHttpImage(uri, auth);
        }
        return image;
    }
    
    private BufferedImage fetchHttpImage(URI uri, Authenticator auth) {
        BufferedImage image = null;
        HttpURLConnection con;
        try {
            URL url = uri.toURL();
            con = (HttpURLConnection) url.openConnection();
            if(auth != null) {
                con.setAuthenticator(auth);
            }

            if (con.getResponseCode() == 200) {

                try (InputStream is = con.getInputStream()) {
                    if (is != null) {
                        image = ImageIO.read(is);
                    }
                }
            } else {
                LOG.fine(format("Cover image missing or connection failed with HTTP response code {0}",
                        con.getResponseCode()));
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error fetching image at: " + uri, e);
        }
        return image;
    }
    
    private BufferedImage fetchLocalImage(URI uri) {
        BufferedImage image = null;
        Path fullPath = Paths.get(uri);
        if(Files.exists(fullPath)) {
            try(InputStream is = Files.newInputStream(fullPath)) {
                if(is != null) {
                    image = ImageIO.read(is);
                }
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Error fetching image at: " + uri, e);
            }
        } else {
            LOG.log(Level.FINE, "Cover image not found at: ''{0}''", fullPath);
        }
        return image;
    }
    
    private static boolean isLocal(URI uri) {
        return "file".equals(uri.getScheme());
    }
    
    private static boolean isHttp(URI uri) {
        return "http".equals(uri.getScheme()) || "https".equals(uri.getScheme());
    }
}
