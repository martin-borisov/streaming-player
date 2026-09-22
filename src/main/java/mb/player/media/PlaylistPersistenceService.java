package mb.player.media;

import java.io.FileInputStream;
import java.io.FileOutputStream;
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
    private static final String DEFAULT_FILE_NAME = "playlist.bin";
    private static final String DEFAULT_FILE_PATH;
    static {
        if(Boolean.valueOf(System.getProperty("mb.config.useHomeDir"))) {
            DEFAULT_FILE_PATH = System.getProperty("user.home") + "/.config/" + DEFAULT_FILE_NAME;
        } else {
            DEFAULT_FILE_PATH = DEFAULT_FILE_NAME;
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
    
    public List<MPMedia> loadPlaylist() {
        return loadPlaylist(DEFAULT_FILE_PATH);
    }
    
    @SuppressWarnings("unchecked")
    public List<MPMedia> loadPlaylist(String path) {
        List<MPMedia> playlist = new ArrayList<>();
        if(Files.exists(Paths.get(path))) {
            try(FileInputStream fis = new FileInputStream(path);
                    ObjectInputStream ois = new ObjectInputStream(fis)) {
                return (List<MPMedia>) ois.readObject();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Playlist file not found", e);
                return null;
            }
        } else {
            LOG.log(Level.WARNING, "Path ''{0}'' does not exist", path);
        }
        return playlist;
    }
    
    public void savePlaylist(List<MPMedia> playlist) {
        savePlaylist(playlist, DEFAULT_FILE_PATH);
    }
    
    public void savePlaylist(List<MPMedia> playlist, String path) {
        try(FileOutputStream fos = new FileOutputStream(path);
                ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(playlist);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

