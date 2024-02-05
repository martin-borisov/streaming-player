package mb.player.media;

import static java.text.MessageFormat.format;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import com.formdev.flatlaf.FlatLightLaf;

import mb.player.components.swing.Playlist;
import mb.player.components.swing.PlaylistModel;
import mb.player.media.audio.AudioPlayer;
import mb.player.media.audio.AudioPlayerException;
import mb.player.media.audio.AudioPlayerListener;
import mb.player.media.audio.AudioSource;
import net.miginfocom.swing.MigLayout;

public class SwingMPlayer extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(SwingMPlayer.class.getName());
    
    private AudioPlayer player;
    private MPMedia currentlyPlayingMedia;
    private MediaPreProcessor currentlyPlayingMpp;
    private Map<String, Object> currMediaAttribs;
    private BufferedImage currArtwork;
    
    private Playlist playlist;
    private JLabel titleLabel, artistAlbumLabel, formatLabel, playTimeLabel;
    private Canvas imageCanvas;
    private JButton playButton;
    private Timer playButtonBlinkTimer;
    private JSlider timeSlider, volumeSlider;
    private JToggleButton loopToggle;
    
    public SwingMPlayer() {
        currMediaAttribs = new HashMap<>();
        createAndLayoutComponents();
        createPlayer();
        createWindowListeners();
        loadStoredPlaylist();
    }
    
    public void playMedia(MPMedia media) {
        currentlyPlayingMedia = media;
        currentlyPlayingMpp = new MediaPreProcessor(media);
        
        // Set credentials for non-local sources, i.e. URLs
        if(!media.isLocal()) {
            setGlobalCredentials(media);
        }
        
        // Open and start player
        try {
            player.open(new AudioSource(media.getSource()));
            player.play();
        } catch (AudioPlayerException e) {
            LOG.log(Level.WARNING, "Audio playback failed", e);
            return;
        }
        
        // Update playlist
        playlist.markAsPlaying(media);
    }
    
    private void createPlayer() {
        player = new AudioPlayer();
        player.addListener(new AudioPlayerListener() {
            public void onOpen(Map<String, Object> properties) {
                onMediaOpened(properties);
            }
            public void onStart() {
                onPlaybackStarted();
            }
            public void onStop() {
                onPlaybackStopped();
            }
            public void onEndOfMedia() {
                SwingMPlayer.this.onEndOfMedia();
            }
            public void onProgress(int elapsedSeconds) {
                // NB: This is called roughly once per second
                onPlayerProgress(elapsedSeconds);
            }
            public void onPause() {
                onPlaybackStopped();
            }
            public void onResume() {
                onPlaybackStarted();
            }
        });
    }
    
    private void createAndLayoutComponents() {
        setLayout(new MigLayout("insets 0, gap 0, wrap, fill", "[]", "[grow][][]"));
        
        /* Playlist */
        add(new JScrollPane(playlist = new Playlist()), "grow");
        playlist.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                if(event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 2) {
                    MPMedia media = playlist.getSelectedValue();
                    if(media != null) {
                        playMedia(media);
                    }
                }
            }
        });
        
        /* Track metadata and artwork panel */
        JPanel trackPanel = new JPanel(new MigLayout("fill, wrap", "[grow][]", "[grow][grow][grow]"));
        add(trackPanel, "grow");
        
        // Title
        titleLabel = new JLabel(" ");
        titleLabel.setFont(new Font(null, Font.BOLD, 18));
        trackPanel.add(titleLabel);
        
        // Artwork
        imageCanvas = new Canvas() {
            private static final long serialVersionUID = 1L;
            
            // NB: Called on repaint
            public void paint(Graphics g) {
                if(currArtwork != null) {
                    
                    int sizeFactor = currArtwork.getHeight() / currArtwork.getWidth();
                    if(getHeight() > getWidth()) {
                        int scaledHeight = getWidth() * sizeFactor;
                        g.drawImage(currArtwork, 0, getHeight() / 2 - scaledHeight / 2, getWidth(), scaledHeight, null);
                    } else {
                        int scaledWidth = getHeight() * sizeFactor;
                        g.drawImage(currArtwork, getWidth() / 2 - scaledWidth / 2, 0, scaledWidth, getHeight(), null);
                    }
                } else {
                    g.clearRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        imageCanvas.setMinimumSize(new Dimension(100, 100)); // This is needed for proper canvas resizing
        trackPanel.add(imageCanvas, "spany 3");
        
        // Artist & album
        artistAlbumLabel = new JLabel(" ");
        trackPanel.add(artistAlbumLabel);
        
        // Format
        formatLabel = new JLabel(" ");
        formatLabel.setFont(new Font(null, Font.ITALIC, artistAlbumLabel.getFont().getSize()));
        trackPanel.add(formatLabel);
        
        
        /* Controls panel */
        JPanel controlsPanel = new JPanel(new MigLayout("wrap, fill", 
                "[grow 1][grow 1][grow 1][align right][grow 10][][align right][][][grow 1]", 
                "[][grow]"));
        add(controlsPanel, "grow");
        
        controlsPanel.add(new JSeparator(SwingConstants.HORIZONTAL), "grow, spanx 10");
        
        // Prev
        JButton prevButton = new JButton(FontIcon.of(FontAwesomeSolid.FAST_BACKWARD, 15));
        prevButton.addActionListener(e -> playPrev());
        controlsPanel.add(prevButton, "grow");
        
        // Play
        controlsPanel.add(playButton = new JButton(FontIcon.of(FontAwesomeSolid.PLAY, 15)), "grow");
        playButton.addActionListener(e -> onPlayButtonClicked());
        createPlayButtonBlinkTimer();
        
        // Next
        JButton nextButton = new JButton(FontIcon.of(FontAwesomeSolid.FAST_FORWARD, 15));
        nextButton.addActionListener(e -> playNext());
        controlsPanel.add(nextButton, "grow");
        
        // Time slider
        controlsPanel.add(new JLabel("Time:"));
        timeSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 0);
        controlsPanel.add(timeSlider, "grow");
        
        // Time label
        playTimeLabel = new JLabel();
        updatePlayTimeLabel(0);
        controlsPanel.add(playTimeLabel);
        
        // Volume
        controlsPanel.add(new JLabel("Vol:"));
        volumeSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 0);
        controlsPanel.add(volumeSlider, "grow");
        
        // Separator
        controlsPanel.add(new JSeparator(SwingConstants.VERTICAL), "grow");
        
        // Loop
        loopToggle = new JToggleButton(FontIcon.of(FontAwesomeSolid.REDO, 15));
        controlsPanel.add(loopToggle, "grow");
    }
    
    private void createWindowListeners() {
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
                System.exit(0);
            }
        });
    }
    
    private void createPlayButtonBlinkTimer() {
        playButtonBlinkTimer = new Timer(100, (event) -> {
            FontIcon icon = (FontIcon) playButton.getIcon();
            Color col = icon.getIconColor();
            col = new Color(col.getRed(), col.getGreen(), col.getBlue(),
                    col.getAlpha() < 255 ? Math.min(col.getAlpha() + 25, 255) : 0);
            icon.setIconColor(col);
            playButton.repaint();
        });
    }
    
    private void setGlobalCredentials(MPMedia media) {
        if(media.getUser() != null && media.getPassword() != null) {
            Authenticator.setDefault(MPUtils.createAuthenticator(media));
        }
    }
    
    private void loadStoredPlaylist() {
        List<MPMedia> pl = PlaylistPersistenceService.getInstance().loadPlaylist();
        if(pl != null) {
            ((PlaylistModel) playlist.getModel()).addAll(pl);
        }
    }
    
    private void playNext() {
        List<MPMedia> tracks = ((PlaylistModel) playlist.getModel()).getAll();
        int idx = tracks.indexOf(currentlyPlayingMedia);
        if(idx > -1 && idx < tracks.size() - 1) {
            playMedia(tracks.get(++idx));
        } else if(idx == tracks.size() - 1 && loopToggle.isSelected()){
            playMedia(tracks.get(0));
        }
    }
    
    private void playPrev() {
        List<MPMedia> tracks = ((PlaylistModel) playlist.getModel()).getAll();
        int idx = tracks.indexOf(currentlyPlayingMedia);
        if(idx > 0) {
            playMedia(tracks.get(--idx));
        }
    }
    
    private void updateTrackMetadata() {
        
        // Title
        String title = (String) currMediaAttribs.get("title");
        if(title == null) {
            if(currentlyPlayingMedia != null) {
                title = currentlyPlayingMedia.getName();
            }
        }
        titleLabel.setText(title);
        
        // Artist & Album
        StringBuilder artistAlbumBuf = new StringBuilder();
        if(currMediaAttribs.containsKey("author") || currMediaAttribs.containsKey("artist")) {
            artistAlbumBuf.append("By '").append(
                    Optional.ofNullable(currMediaAttribs.get("author"))
                        .orElse(currMediaAttribs.get("artist"))).append("' ");
        }
        if(currMediaAttribs.containsKey("album")) {
            artistAlbumBuf.append("from album '").append(currMediaAttribs.get("album")).append("'");
        }
        artistAlbumLabel.setText(artistAlbumBuf.toString());
        
        // Format
        StringBuilder buf = new StringBuilder();
        if(currMediaAttribs.containsKey("audio.type")) {
            buf.append(currMediaAttribs.get("audio.type").toString()).append(" | ");
            buf.append(format("{0,number,#} Hz | {1} bit | {2} channels", currMediaAttribs.get("audio.samplerate.hz"), 
                    currMediaAttribs.get("audio.samplesize.bits"), currMediaAttribs.get("audio.channels")));
            
            if(currMediaAttribs.containsKey("bitrate")) {
                buf.append(format(" | {0,number,#} kbs", Integer.valueOf(currMediaAttribs.get("bitrate").toString()) / 1000));
            }
            
            if(Boolean.valueOf(String.valueOf(currMediaAttribs.get("vbr")))) {
                buf.append(" vbr");                
            }
        }
        formatLabel.setText(buf.toString());
    }
    
    public void close() {
        player.stop();
        
        // Keep playlist
        PlaylistPersistenceService.getInstance().savePlaylist(
                ((PlaylistModel) playlist.getModel()).getAll());
    }
    
    /* Event Handlers */
    
    private void onPlayButtonClicked() {
        
        // Pause current, resume current or play first media in playlist
        try {
            if(player.isPlaying()) {
                player.pause();
            } else if(player.isPaused()) {
                player.resume();
            } else if(currentlyPlayingMedia == null && playlistNotEmpty()){
                MPMedia selected = playlist.getModel().getElementAt(0);
                if(selected != null) {
                    playMedia(selected);
                }
            } 
        } catch (AudioPlayerException e) {
            LOG.log(Level.WARNING, "Audio player error", e);
        }
    }
    
    private void onMediaOpened(Map<String, Object> properties) {
        
        // Append attributes from player and preprocessor
        currMediaAttribs = new HashMap<>(properties);
        currMediaAttribs.putAll(currentlyPlayingMpp.getAttributes());
        
        // Fetch artwork
        try {
            currArtwork = MPUtils.fetchMediaCoverArtSwing(currentlyPlayingMedia);
            if(currArtwork == null) {
                currArtwork = MPUtils.imageFromID3TagSwing(
                        (ByteArrayInputStream) currMediaAttribs.get("mp3.id3tag.v2"));
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Loading album art failed", e);
            currArtwork = null;
        }
        
        SwingUtilities.invokeLater(() -> {
            updateTrackMetadata();
            timeSlider.setMaximum((int) (currentlyPlayingMpp.getDurationSec() > 0 ? 
                    currentlyPlayingMpp.getDurationSec() : 0));
            volumeSlider.setValue((int) (player.getVolume() * 100));
            imageCanvas.repaint();
        });
    }
    
    private void onPlaybackStarted() {
        startPlayButtonBlink();
    }
    
    private void onPlaybackStopped() {
        stopPlayButtonBlink();
    }
    
    private void onEndOfMedia() {
        playNext();
    }
    
    private void onPlayerProgress(int elapsedSeconds) {
        SwingUtilities.invokeLater(() -> {
            
            // Update only if user is not currently dragging the slider
            if(!timeSlider.getValueIsAdjusting()) {
                
                // Update elapsed time label (even if duration is not known)
                updatePlayTimeLabel(elapsedSeconds);
            
                // Update progress slider (duration must be known)
                if(currentlyPlayingMpp.getDurationSec() > 0) {
                    timeSlider.setValue(elapsedSeconds);
                }
            }
        });
    }
    
    /* Utils */
    
    private void startPlayButtonBlink() {
        ((FontIcon) playButton.getIcon()).setIconColor(Color.GREEN);
        playButtonBlinkTimer.restart();
    }
    
    private void stopPlayButtonBlink() {
        ((FontIcon) playButton.getIcon()).setIconColor(Color.BLACK);
        playButtonBlinkTimer.stop();
    }
    
    private boolean playlistNotEmpty() {
        return playlist.getModel().getSize() > 0;
    }
    
    private void updatePlayTimeLabel(int seconds) {
        int hrs = (seconds / 60) / 60;
        int min = (seconds / 60) % 60;
        int sec = seconds % 60;
        playTimeLabel.setText(MessageFormat.format("{0}{1}:{2}{3}:{4}{5}", 
                hrs < 10 ? "0" : "", hrs, 
                min < 10 ? "0" : "", min, 
                sec < 10 ? "0" : "", sec));
    }

    /* Main */
    
    public static void main(String[] args) {
        FlatLightLaf.setup();
        
        SwingMPlayer player = new SwingMPlayer();
        player.setSize(800, 600);
        player.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        player.setVisible(true);
    }
}
