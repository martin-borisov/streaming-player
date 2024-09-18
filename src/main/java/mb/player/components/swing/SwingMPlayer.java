package mb.player.components.swing;

import static java.text.MessageFormat.format;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Taskbar;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import com.beust.jcommander.JCommander;
import com.formdev.flatlaf.FlatLightLaf;
import com.github.sardine.DavResource;
import com.github.sardine.SardineFactory;

import mb.player.components.swing.properties.PropertyService;
import mb.player.media.MPMedia;
import mb.player.media.MPUtils;
import mb.player.media.MediaPreProcessor;
import mb.player.media.PlaylistPersistenceService;
import mb.player.media.audio.AudioPlayer;
import mb.player.media.audio.AudioPlayerException;
import mb.player.media.audio.AudioPlayerListener;
import mb.player.media.audio.AudioSource;
import mb.player.media.audio.AudioSystemWrapper;
import mb.player.media.audio.DummySourceDataLine;
import net.miginfocom.swing.MigLayout;

public class SwingMPlayer extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(SwingMPlayer.class.getName());
    private static final int BUTTON_ICON_SIZE = 15;
    
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
        createConfigListeners();
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
            player.setVolume(volumeSlider.getValue());
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
                    
                    int sizeFactor = Math.round((float) currArtwork.getHeight() / (float) currArtwork.getWidth());
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
                "[grow 1][grow 1][grow 1][align right][grow 10][][align right][grow 1][grow 1][grow 1][grow 1][grow 1]", 
                "[][grow]"));
        add(controlsPanel, "grow");
        
        controlsPanel.add(new JSeparator(SwingConstants.HORIZONTAL), "grow, spanx 12");
        
        // Prev
        JButton prevButton = new JButton(FontIcon.of(FontAwesomeSolid.FAST_BACKWARD, BUTTON_ICON_SIZE));
        prevButton.addActionListener(e -> playPrev());
        controlsPanel.add(prevButton, "grow");
        
        // Play
        controlsPanel.add(playButton = new JButton(FontIcon.of(FontAwesomeSolid.PLAY, BUTTON_ICON_SIZE)), "grow");
        playButton.addActionListener(e -> onPlayButtonClicked());
        createPlayButtonBlinkTimer();
        
        // Next
        JButton nextButton = new JButton(FontIcon.of(FontAwesomeSolid.FAST_FORWARD, BUTTON_ICON_SIZE));
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
        
        // Separator
        controlsPanel.add(new JSeparator(SwingConstants.VERTICAL), "grow");
        
        // Volume
        volumeSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 0, 0);
        volumeSlider.addChangeListener(e -> onVolumeSliderMoved());
        
        JMenuItem volumeMenu = new JMenuItem();
        volumeMenu.add(volumeSlider);
        volumeMenu.setEnabled(false);
        JPopupMenu volumePopup = new JPopupMenu();
        volumePopup.add(volumeMenu);
        volumePopup.setPreferredSize(new Dimension(150, (int) volumePopup.getPreferredSize().getHeight()));
        
        
        // TODO Change volume button icon as volume changes
        JButton volumeButton = new JButton(FontIcon.of(FontAwesomeSolid.VOLUME_UP, BUTTON_ICON_SIZE));
        volumeButton.setComponentPopupMenu(volumePopup);
        
        volumeButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                volumePopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        controlsPanel.add(volumeButton, "grow");
        
        // Loop
        loopToggle = new JToggleButton(FontIcon.of(FontAwesomeSolid.REDO, BUTTON_ICON_SIZE));
        loopToggle.setSelected((boolean) PropertyService.getInstance().getOrCreateProperty(
                PropertyNamesConst.LOOP_PLAYLIST_PROPERTY_NAME, false).getRight());
        loopToggle.addActionListener(e -> PropertyService.getInstance().setProperty(
                new MutablePair<>(PropertyNamesConst.LOOP_PLAYLIST_PROPERTY_NAME, loopToggle.isSelected())));
        controlsPanel.add(loopToggle, "grow");
        
        // Add local
        JButton addLocalButton = new JButton(FontIcon.of(FontAwesomeSolid.PLUS, BUTTON_ICON_SIZE));
        addLocalButton.addActionListener(e -> onAddLocalButtonClicked());
        controlsPanel.add(addLocalButton, "grow");
        
        // Add remote
        JButton addRemoteButton = new JButton(FontIcon.of(FontAwesomeSolid.GLOBE, BUTTON_ICON_SIZE));
        addRemoteButton.addActionListener(e -> onAddRemoteButtonClicked());
        controlsPanel.add(addRemoteButton, "grow");
        
        // Settings
        JButton settingsButton = new JButton(FontIcon.of(FontAwesomeSolid.COG, BUTTON_ICON_SIZE));
        settingsButton.addActionListener(e -> onSettingsButtonClicked());
        controlsPanel.add(settingsButton, "grow");
    }
    
    private void createWindowListeners() {
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
                System.exit(0);
            }
        });
    }
    
    private void createConfigListeners() {
        PropertyService ps = PropertyService.getInstance();
        ps.addPropertyChangeListener(e -> {
            switch (e.getPropertyName()) {
                case PropertyNamesConst.LOOP_PLAYLIST_PROPERTY_NAME:
                    SwingUtilities.invokeLater(() -> {
                        loopToggle.setSelected((Boolean) e.getNewValue());
                    });
                    break;
                    
                case PropertyNamesConst.SHOW_MEDIA_ATTRIBUTES_PROPERTY_NAME:
                    SwingUtilities.invokeLater(() -> {
                        ((PlaylistModel) playlist.getModel()).refresh();
                    });
                    break;
            }
        });
    }
    
    private void createPlayButtonBlinkTimer() {
        playButtonBlinkTimer = new Timer(100, e -> {
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
        
        // Handled cases:
        // * Play next
        // * Play first if previous was last in list
        // * Play selected if current was deleted
        // * Play first if everything else fails
        if(idx > -1 && idx < tracks.size() - 1) {
            playMedia(tracks.get(++idx));
        } else if(idx == tracks.size() - 1 && loopToggle.isSelected()){
            playMedia(tracks.get(0));
        } else if(idx == -1 && playlist.getSelectedValue() != null){
            playMedia(playlist.getSelectedValue());
        } else if(!tracks.isEmpty() && loopToggle.isSelected()) {
            playMedia(tracks.get(0));
        } else {
            onPlaybackStopped();
        }
    }
    
    private void playPrev() {
        List<MPMedia> tracks = ((PlaylistModel) playlist.getModel()).getAll();
        int idx = tracks.indexOf(currentlyPlayingMedia);
        
        // Handled cases:
        // * Play previous
        // * Play selected if current was deleted
        // * Play first if everything else fails
        if(idx > 0) {
            playMedia(tracks.get(--idx));
        } else if(idx == -1 && playlist.getSelectedValue() != null){
            playMedia(playlist.getSelectedValue());
        } else if(!tracks.isEmpty()) {
            playMedia(tracks.get(0));
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
            artistAlbumBuf.append("from '").append(currMediaAttribs.get("album")).append("'");
        }
        artistAlbumLabel.setText(artistAlbumBuf.toString());
        
        // Format
        StringBuilder buf = new StringBuilder();
        if(currMediaAttribs.containsKey("audio.type")) {
            buf.append(currMediaAttribs.get("audio.type").toString()).append(" | ");
            buf.append(format("{0} Hz | {1} bit | {2} channels", currMediaAttribs.get("audio.samplerate.hz"), 
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
            volumeSlider.setMinimum((int) player.getMinVolume());
            volumeSlider.setMaximum((int) player.getMaxVolume());
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
    
    private void onAddLocalButtonClicked() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Supported Audio Files", "mp3", "flac", "wav"));
        if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            
            File[] files = chooser.getSelectedFiles();
            List<MPMedia> media = Arrays.stream(files)
                .filter(f -> f.getName().endsWith("mp3") || f.getName().endsWith("flac") || f.getName().endsWith("wav"))
                .map(f -> new MPMedia(f.getName(), f.toURI().toString(), MPMedia.Type.AUDIO))
                .sorted((m1, m2) -> StringUtils.compare(m1.getName(), m2.getName()))
                .collect(Collectors.toList());
            ((PlaylistModel) playlist.getModel()).addAll(media);
        }
    }
    
    private void onAddRemoteButtonClicked() {
        String[] input = LoginDialog.showDialog(this);
        if(input != null) {
            String path = input[0], user = input[1], pass = input[2];
            List<MPMedia> media = fetchRemoteMedia(path, user, pass);
            if(media != null) {
                ((PlaylistModel) playlist.getModel()).addAll(media);
            }
        }
    }
    
    private void onSettingsButtonClicked() {
        SettingsDialog.showDialog(this);
    }
    
    private void onVolumeSliderMoved() {
        if(!volumeSlider.getValueIsAdjusting()) {
            player.setVolume(volumeSlider.getValue());
        }
    }
    
    /* Utils */
    
    private void startPlayButtonBlink() {
        ((FontIcon) playButton.getIcon()).setIconColor(Color.GREEN);
        playButtonBlinkTimer.restart();
    }
    
    private void stopPlayButtonBlink() {
        playButtonBlinkTimer.stop();
        ((FontIcon) playButton.getIcon()).setIconColor(Color.BLACK);
        playButton.repaint();
    }
    
    private boolean playlistNotEmpty() {
        return playlist.getModel().getSize() > 0;
    }
    
    private void updatePlayTimeLabel(int seconds) {
        playTimeLabel.setText(MPUtils.secToTimeFormat(seconds));
    }
    
    private List<MPMedia> fetchRemoteMedia(String path, String user, String pass) {
        List<MPMedia> media = null;
        List<DavResource> resources = fetchRemoteResources(path, user, pass);
        if(resources != null) {
        
            // Filter out mp3 and flac files and add them to the playlist
            media = resources.stream()
                    .filter(r -> r.getName().endsWith("mp3") || r.getName().endsWith("flac") || r.getName().endsWith("wav"))
                    .map(r -> new MPMedia(r.getName(), path + "/" + MPUtils.encodeUrlPath(r.getName()), user, pass, MPMedia.Type.AUDIO))
                    .sorted((m1, m2) -> StringUtils.compare(m1.getName(), m2.getName()))
                    .collect(Collectors.toList());
        }
        return media;
    }
    
    private List<DavResource> fetchRemoteResources(String path, String user, String pass) {
        List<DavResource> resources = null;
        try {
            resources = SardineFactory.begin(user, pass).list(path);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Adding remote location failed", e);
            JOptionPane.showMessageDialog(null, e.getMessage() != null ? e.getMessage() : 
                (e.getCause() != null ? e.getCause().getMessage() : "Unknown error"), 
                    "Remote Location Error", JOptionPane.WARNING_MESSAGE);
        }
        return resources;
    }

    /* Main */
    
    public static void main(String[] args) {
        
        // Parse command
        Args arg = new Args();
        JCommander.newBuilder().addObject(arg).build().parse(args);
        
        if(arg.isMockAudio()) {
            
            // Mock out the underlying audio system for debug purposes
            AudioSystemWrapper.setDefaultSourceDataLine(new DummySourceDataLine());
            LOG.log(Level.INFO, "Mock audio system enabled");
        }
        
        BufferedImage image = null;
        try {
            image = ImageIO.read(SwingMPlayer.class.getResource(
                    "/streaming-player-icon.png"));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Loading app icon failed", e);
        }
        
        
        // Set application taskbar/dock icon
        if(Taskbar.isTaskbarSupported() && image != null) {
            try {
                Taskbar.getTaskbar().setIconImage(image);
            } catch (UnsupportedOperationException e) {
                LOG.log(Level.FINE, e.getMessage());
            }
        }
        
        // Customize application about menu
        if(Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().setAboutHandler(e -> {
                    JOptionPane.showMessageDialog(null, "Streaming Player v1.0", 
                            "About Streaming Player", JOptionPane.INFORMATION_MESSAGE, null);
                });
            } catch (UnsupportedOperationException e) {
                LOG.log(Level.FINE, e.getMessage());
            }
        }
        
        FlatLightLaf.setup();
        SwingMPlayer player = new SwingMPlayer();
        player.setSize(800, 600);
        player.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        player.setLocationByPlatform(true);
        player.setTitle("Streaming Player");
        
        if(image != null) {
            player.setIconImage(image);
        }
        
        player.setVisible(true);
    }
}
