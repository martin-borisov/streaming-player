package mb.player.media;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.Serializable;

public class MPMedia implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Location {
        LOCAL, REMOTE
    }
    
    public enum Type {
        AUDIO, VIDEO
    }
    
    private String name, source, user, password;
    private Type type;
    private long durationSec;
    private String title, album, artist;
    private transient BufferedImage artwork;
    private transient Image artworkThumb;
    
    public MPMedia(String name, String source, Type type) {
        this.name = name;
        this.source = source;
        this.type = type;
    }

    public MPMedia(String name, String source, String user, String password, Type type) {
        this(name, source, type);
        this.user = user;
        this.password = password;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
    
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isLocal() {
        return source != null && source.startsWith("file");
    }

    public long getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(long durationSec) {
        this.durationSec = durationSec;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public BufferedImage getArtwork() {
        return artwork;
    }

    public void setArtwork(BufferedImage artwork) {
        this.artwork = artwork;
    }

    public Image getArtworkThumb() {
        return artworkThumb;
    }

    public void setArtworkThumb(Image artworkThumb) {
        this.artworkThumb = artworkThumb;
    }
    
    @Override
    public String toString() {
        return "MPMedia [name=" + name + ", source=" + source + ", type=" + type + "]";
    }
}
