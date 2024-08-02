package mb.player.media;

import static java.text.MessageFormat.format;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.mpatric.mp3agic.AbstractID3v2Tag;
import com.mpatric.mp3agic.ID3v2TagFactory;

public class MPUtils {
    
    private static final Logger LOG = Logger.getLogger(MPUtils.class.getName());
    
    public static InputStream imageInputStreamFromID3Tag(ByteArrayInputStream bytes) {
        InputStream is = null;
        if(bytes != null) {
            try {
                AbstractID3v2Tag tag = ID3v2TagFactory.createTag(bytes.readAllBytes());
                byte[] imageData = tag.getAlbumImage();
                if(imageData != null) {
                    is = new ByteArrayInputStream(imageData);
                    
                    // Dump some useful data
                    LOG.fine(format("Album art mime type: {0}", tag.getAlbumImageMimeType()));
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        }
        return is;
    }
    
    public static BufferedImage imageFromID3TagSwing(ByteArrayInputStream bytes) throws IOException {
        try(InputStream is = imageInputStreamFromID3Tag(bytes)) {
            BufferedImage image = null;
            if(is != null) {
                image = ImageIO.read(is);
            }
            return image;
        }
    }
    
    public static InputStream fetchMediaCoverArtInputStream(MPMedia media) throws IOException {
        
        InputStream is = null;
        String source = media.getSource();
        int idx = source.lastIndexOf('/');
        if(idx > -1) {
            String path = source.substring(0, idx + 1); // Keep the forward slash
        
            if(media.isLocal()) {
                
                Path fullPath = Paths.get(URI.create(path + "cover.jpg"));
                if(Files.exists(fullPath)) {
                    is = Files.newInputStream(fullPath);
                } else {
                    LOG.fine(format("Cover image not found at: {0}", fullPath));
                }

            } else {
                
                URL url = new URL(path + "cover.jpg");
                LOG.fine(format("Trying to fetch cover image at URL: {0}", url));
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setAuthenticator(createAuthenticator(media));

                if (con.getResponseCode() == 200) {
                    return con.getInputStream();
                } else {
                    LOG.fine(format("Cover image missing or connection failed with response code {0}",
                            con.getResponseCode()));
                }
            }
        }
        return is;
    }
    
    public static BufferedImage fetchMediaCoverArtSwing(MPMedia media) throws IOException {
        try(InputStream is = fetchMediaCoverArtInputStream(media)) {
            BufferedImage image = null;
            if(is != null) {
                image = ImageIO.read(is);
            }
            return image;
        }
    }
    
    public static Authenticator createAuthenticator(MPMedia media) {
        return new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(media.getUser(), media.getPassword().toCharArray());
            }
        };
    }
    
    public static Optional<String> getFileExtension(File file) {
        return Optional.ofNullable(file.getName())
                .filter(name -> name.contains("."))
                .map(name -> name.substring(name.lastIndexOf(".") + 1));
    }
    
    public static boolean isMedia(File file) {
        final List<String> extensions = Arrays.asList("mp3", "flac", "wav");
        Optional<String> ext = getFileExtension(file);
        return extensions.contains(ext.map(s -> s.toLowerCase()).orElse(""));
    }
    
    /**
     * Properly encodes URI path segments
     */
    public static String encodeUrlPath(String pathSegment) {
        final StringBuilder sb = new StringBuilder();

        try {
            for (int i = 0; i < pathSegment.length(); i++) {
                final char c = pathSegment.charAt(i);

                if (((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z')) || ((c >= '0') && (c <= '9')) || (c == '-')
                        || (c == '.') || (c == '_') || (c == '~')) {
                    sb.append(c);
                } else {
                    final byte[] bytes = String.valueOf(c).getBytes("UTF-8");
                    for (byte b : bytes) {
                        sb.append('%').append(Integer.toHexString((b >> 4) & 0xf)).append(Integer.toHexString(b & 0xf));
                    }
                }
            }

            return sb.toString();
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
