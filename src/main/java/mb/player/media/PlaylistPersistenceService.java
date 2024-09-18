package mb.player.media;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlaylistPersistenceService {
    private static final Logger LOG = 
            Logger.getLogger(PlaylistPersistenceService.class.getName());
    private static PlaylistPersistenceService ref;
    private static final String FILE_NAME = "playlist.bin";
    private static final String FILE_PATH;
    static {
        if(Boolean.valueOf(System.getProperty("mb.config.useHomeDir"))) {
            FILE_PATH = System.getProperty("user.home") + "/.config/" + FILE_NAME;
        } else {
            FILE_PATH = FILE_NAME;
        }
    }
    
    public static PlaylistPersistenceService getInstance() {
        synchronized (PlaylistPersistenceService.class) {
            if (ref == null) {
                ref = new PlaylistPersistenceService();
            }
        }
        return ref;
    }
    
    private PlaylistPersistenceService() {
    }
    
    @SuppressWarnings("unchecked")
    public List<MPMedia> loadPlaylist() {
        List<MPMedia> playlist = new ArrayList<>();
        if(Files.exists(Paths.get(FILE_PATH))) {
            
            FileInputStream fis = null;
            ObjectInputStream ois = null;
            try {
                fis = new FileInputStream(FILE_PATH);
                ois = new ObjectInputStream(fis);
                return (List<MPMedia>) ois.readObject();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Playlist file not found", e);
                return null;
            } finally {
                if(ois != null) {
                    try {
                        ois.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
                if(fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }
        return playlist;
    }
    
    public void savePlaylist(List<MPMedia> playlist) {
        
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(FILE_PATH);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(playlist);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if(fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }
}

