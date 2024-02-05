package mb.player.media;

import static java.text.MessageFormat.format;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.Authenticator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import mb.player.components.ComponentUtils;
import mb.player.components.Icons;
import mb.player.media.audio.AudioPlayer;
import mb.player.media.audio.AudioPlayerException;
import mb.player.media.audio.AudioPlayerListener;
import mb.player.media.audio.AudioSource;

public class MPlayer extends Application {
    private static final Logger LOG = Logger.getLogger(MPlayer.class.getName());
    
    private Button playButton;
    private Slider timeSlider, volumeSlider;
    private Label playTime;
    private ToggleButton loopToggle;
    private ListView<MPMedia> playlist;
    private MPMedia currentlyPlayingMedia;
    private MediaPreProcessor currentlyPlayingMpp;
    private AudioPlayer player;
    private SimpleObjectProperty<Map<String, Object>> currMediaAttribsProperty;
    
    // TODO 1) Mp3 tags/properties 2) Duration of local mp3 and wav 3) Ability to seek for local media
    
    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Audio Player");
        createProperties();
        createAudioPlayer();
        setupScene(stage);
        loadStoredPlaylist();
    }
    
    @Override
    public void stop() throws Exception {
        destroy();
        
        // And this is needed as there are some leftover non-daemon threads in the media player library
        System.exit(0);
    }

    public void addToPlaylist(MPMedia media) {
        playlist.getItems().add(media);
    }
    
    public boolean removeFromPlaylist(MPMedia media) {
        return playlist.getItems().remove(media);
    }
    
    public void clearPlaylist() {
        playlist.getItems().clear();
    }
    
    private void createProperties() {
        currMediaAttribsProperty = new SimpleObjectProperty<Map<String,Object>>(
                Collections.emptyMap());
    }
    
    private void createAudioPlayer() {
        player = new AudioPlayer();
        player.addListener(new AudioPlayerListener() {
            public void onOpen(Map<String, Object> properties) {
                onPlayerOpened(properties);
            }
            public void onStart() {
                onPlayerStarted();
            }
            public void onStop() {
                onPlayerStopped();
            }
            public void onEndOfMedia() {
                MPlayer.this.onEndOfMedia();
            }
            public void onProgress(int elapsedSeconds) {
                // NB: This is called roughly once per second
                onPlayerProgress(elapsedSeconds);
            }
            public void onPause() {
                onPlayerStopped();
            }
            public void onResume() {
                onPlayerStarted();
            }
        });
    }
    
    public void destroy() {
        player.stop();
        
        // Keep playlist
        PlaylistPersistenceService.getInstance().savePlaylist(new ArrayList<MPMedia>(playlist.getItems()));
    }
    
    /* Create and setup UI */
    
    private void setupScene(Stage stage) {
        BorderPane borderPane = new BorderPane();
        
        // Playlist
        ScrollPane scrollPane = new ScrollPane(createPlaylist());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        borderPane.setCenter(scrollPane);
        
        // Control layout
        MigPane mig = new MigPane("fill, wrap 2",
                "[left, grow][]", "top");
        
        // Title
        Label titleLabel = new Label();
        titleLabel.setFont(Font.font(null, FontWeight.BOLD, 18));
        titleLabel.textProperty().bind(Bindings.createObjectBinding(() -> {
            String title = (String) currMediaAttribsProperty.get().get("title");
            if(title == null) {
                if(currentlyPlayingMedia != null) {
                    title = currentlyPlayingMedia.getName();
                }
            }
            return title;
        }, currMediaAttribsProperty));
        
        // Artist & Album
        Label artistAlbumLabel = new Label();
        artistAlbumLabel.textProperty().bind(Bindings.createObjectBinding(() -> {
            
            StringBuilder buf = new StringBuilder();
            Map<String, Object> props = currMediaAttribsProperty.get();
            if(props.containsKey("author") || props.containsKey("artist")) {
                buf.append("By '").append(
                        Optional.ofNullable(props.get("author")).orElse(props.get("artist"))).append("' ");
            }
            if(props.containsKey("album")) {
                buf.append("from album '").append(props.get("album")).append("'");
            }
            return buf.toString();
        }, currMediaAttribsProperty));
        
        // Format
        Label formatLabel = new Label();
        formatLabel.setFont(Font.font(null, FontPosture.ITALIC, -1));
        formatLabel.textProperty().bind(Bindings.createObjectBinding(() -> {
            
            Map<String, Object> tags = currMediaAttribsProperty.get();
            StringBuilder buf = new StringBuilder();
            
            if(tags.containsKey("audio.type")) {
                buf.append(tags.get("audio.type").toString()).append(" | ");
                buf.append(format("{0,number,#} Hz | {1} bit | {2} channels", 
                        tags.get("audio.samplerate.hz"), tags.get("audio.samplesize.bits"), tags.get("audio.channels")));
                
                if(tags.containsKey("bitrate")) {
                    buf.append(format(" | {0,number,#} kbs", Integer.valueOf(tags.get("bitrate").toString()) / 1000));
                }
                
                if(Boolean.valueOf(String.valueOf(tags.get("vbr")))) {
                    buf.append(" vbr");                
                }
            }
            return buf.toString();
        }, currMediaAttribsProperty));
        
        VBox tagsBox = new VBox(titleLabel, artistAlbumLabel, formatLabel);
        mig.add(tagsBox);
        
        // Album art
        ImageView imageView = new ImageView();
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.imageProperty().bind(Bindings.createObjectBinding(() -> {
            Image image = null;
            
            // Try to fetch cover image from file and as a second option from the ID3 tag
            if(currentlyPlayingMedia != null) {
                image = MPUtils.fetchMediaCoverArtJavaFX(currentlyPlayingMedia);
                if(image == null) {
                    image = MPUtils.imageFromID3TagJavaFX(
                            (ByteArrayInputStream) currMediaAttribsProperty.getValue().get("mp3.id3tag.v2"));
                }
            }
            return image;
        }, currMediaAttribsProperty));
        mig.add(imageView);
        
        // Playback controls
        mig.add(new Separator(Orientation.HORIZONTAL), "span 2, growx");
        HBox mediaBarBox = new HBox();
        mediaBarBox.setPadding(new Insets(5, 10, 5, 10));
        mediaBarBox.setSpacing(5);
        mediaBarBox.setAlignment(Pos.CENTER);
        mig.add(mediaBarBox, "span 2, growx");
        borderPane.setBottom(mig);

        // Buttons
        mediaBarBox.getChildren().addAll(
                createFBButton(), createPlayButton(), createFFButton());
        
        // Transport slider
        mediaBarBox.getChildren().add(new Label("Time: "));
        mediaBarBox.getChildren().add(createTimeSlider());
        HBox.setHgrow(timeSlider, Priority.ALWAYS);

        // Play time label
        playTime = new Label();
        playTime.setPrefWidth(130);
        playTime.setMinWidth(50);
        mediaBarBox.getChildren().add(playTime);

        // Volume slider
        mediaBarBox.getChildren().add(new Label("Vol: "));
        mediaBarBox.getChildren().add(createVolumeSlider());
        
        // Info
        Button infoButton = new Button("", Icons.info());
        infoButton.disableProperty().bind(Bindings.createObjectBinding(() -> {
            return currMediaAttribsProperty.getValue().isEmpty();
        }, currMediaAttribsProperty));
        infoButton.setOnAction(event -> {
            ComponentUtils.showMapPropertiesDialog(currMediaAttribsProperty.getValue(), 
                    format("Properties of ''{0}''", currentlyPlayingMedia.getName()));
        });
        mediaBarBox.getChildren().add(infoButton);
        
        // Clear
        Button clear = new Button("", new FontIcon(FontAwesomeRegular.TRASH_ALT));
        clear.setOnAction(event -> {
            clearPlaylist();
        });
        
        // Loop
        loopToggle = new ToggleButton("", new FontIcon(FontAwesomeSolid.REDO));
        
        // TODO Enable after porting config service
        /*
        loopToggle.setSelected(Boolean.valueOf(
                ConfigService.getInstance().getOrCreateProperty("mplayer.loop", Boolean.FALSE.toString())));
        loopToggle.setOnAction(event -> {
            ConfigService.getInstance().setProperty("mplayer.loop", String.valueOf(loopToggle.isSelected()));
        });
        */
        
        mediaBarBox.getChildren().addAll(new Separator(Orientation.VERTICAL), clear, loopToggle);
        
        // Scene
        stage.setWidth(600);
        stage.setHeight(400);
        stage.setScene(new Scene(borderPane));
        stage.show();
    }
    
    private Button createPlayButton() {
        playButton = new Button("", Icons.play());
        playButton.setOnAction(event -> {
            onPlayButtonClicked();
        });
        return playButton;
    }
    
    private Button createFFButton() {
        Button b = new Button("", Icons.fastForward());
        b.setOnAction(event -> {
            try {
                playNext();
            } catch (Exception e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        });
        return b;
    }
    
    private Button createFBButton() {
        Button b = new Button("", Icons.fastBackward());
        b.setOnAction(event -> {
            try {
                playPrev();
            } catch (Exception e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        });
        return b;
    }
    
    private Slider createTimeSlider() {
        timeSlider = new Slider();
        timeSlider.setMinWidth(50);
        timeSlider.setMaxWidth(Double.MAX_VALUE);
        timeSlider.setMax(1);
        //timeSlider.setDisable(true);
        
        // Update seconds counter while dragging
        timeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updatePlayTimeLabel(newVal.intValue());
        });
        
        // Update final value
        timeSlider.setOnMouseReleased(event -> {
            onTimeSliderValueChangedByUser(timeSlider.getValue());
        });
        return timeSlider;
    }
    
    private Slider createVolumeSlider() {
        volumeSlider = new Slider(0, 100, player.getVolume());
        volumeSlider.setPrefWidth(70);
        volumeSlider.setMaxWidth(Region.USE_PREF_SIZE);
        volumeSlider.setMinWidth(30);
        volumeSlider.setOnMouseReleased(event -> {
            player.setVolume((float) (volumeSlider.getValue() / 100));
        });
        return volumeSlider;
    }
    
    private ListView<MPMedia> createPlaylist() {
        playlist = new ListView<>();
        
        // Custom list cell
        playlist.setCellFactory(value -> {
            return new MPMediaListCell() {
                protected void updateItem(MPMedia item, boolean empty) {
                    updateItem(item, empty, currentlyPlayingMedia);
                }
            };
        });
        
        // Double click
        playlist.setOnMouseClicked(event -> {
            MPMedia media = playlist.getSelectionModel().getSelectedItem();
            if (event.getClickCount() == 2 && media != null) {
                playMedia(media);
            }
        });
        
        // Context menu
        MenuItem removeMenuItem = new MenuItem("Remove");
        removeMenuItem.setOnAction((event) -> {
            removeSelectedFromPlaylist();
        });
        playlist.setContextMenu(new ContextMenu(removeMenuItem));
        
        // Drag and drop support
        playlist.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            
            // Check mime type for supported formats
            if(db.hasFiles()) {
                for(File file : db.getFiles()) {
                    if(MPUtils.isMedia(file)) {
                        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                        event.consume();
                        break;
                    }
                }
            }
        });
        
        playlist.setOnDragDropped(event -> {
            for (File file : event.getDragboard().getFiles()) {
                if(MPUtils.isMedia(file)) {
                    addToPlaylist(new MPMedia(file.getName(), file.toURI().toString(), MPMedia.Type.AUDIO));
                }
            }
            event.consume();
        });
        
        return playlist;
    }
    
    private void loadStoredPlaylist() {
        List<MPMedia> pl = PlaylistPersistenceService.getInstance().loadPlaylist();
        if(pl != null) {
            playlist.getItems().addAll(pl);
        }
    }
    
    /* Event Handlers */
    
    private void onPlayButtonClicked() {
        
        // Pause current, resume current or play first media in playlist
        try {
            if(player.isPlaying()) {
                player.pause();
            } else if(player.isPaused()) {
                player.resume();
            } else if(currentlyPlayingMedia == null && !playlist.getItems().isEmpty()){
                MPMedia selected = playlist.getItems().get(0);
                if(selected != null) {
                    playMedia(selected);
                }
            } 
        } catch (AudioPlayerException e) {
            LOG.log(Level.WARNING, "Audio player error", e);
        }
    }
    
    private void onTimeSliderValueChangedByUser(double newVal) {
        LOG.fine(format("Trying to seek to second {0}", (int) newVal));
        if(currentlyPlayingMpp.getDurationSec() > 0) {
            try {
                player.seekTo((int) newVal);
            } catch (AudioPlayerException e) {
                LOG.log(Level.INFO, "Seeking track failed", e);
            }
        }
    }
    
    private void onPlayerOpened(Map<String, Object> properties) {
        
        // Append attributes from player and preprocessor
        Map<String , Object> attributes = new HashMap<String, Object>(properties);
        attributes.putAll(currentlyPlayingMpp.getAttributes());
        
        Platform.runLater(() -> {
            currMediaAttribsProperty.set(attributes);
            timeSlider.setMax(currentlyPlayingMpp.getDurationSec() > 0 ? 
                    currentlyPlayingMpp.getDurationSec() : 0);
            volumeSlider.setValue(player.getVolume() * 100);
        });
    }
    
    private void onPlayerStarted() {
        Platform.runLater(() -> {
            playButton.setGraphic(Icons.pause());
        });
    }
    
    private void onPlayerStopped() {
        Platform.runLater(() -> {
            playButton.setGraphic(Icons.play());
        });
    }
    
    private void onPlayerProgress(int elapsedSeconds) {
        Platform.runLater(() -> {
            
            // Update only if user is not currently dragging the slider
            if(!timeSlider.isValueChanging()) {
                
                // Update elapsed time label (even if duration is not known)
                updatePlayTimeLabel(elapsedSeconds);
            
                // Update progress slider (duration must be known)
                if(currentlyPlayingMpp.getDurationSec() > 0) {
                    timeSlider.setValue(elapsedSeconds);
                }
            }
        });
    }
    
    private void onEndOfMedia() {
        playNext();
    }
    
    /* Utilities */
    
    private void playNext() {
        ObservableList<MPMedia> items = playlist.getItems();
        int idx = items.indexOf(currentlyPlayingMedia);
        if(idx > -1 && idx < items.size() - 1) {
            playMedia(items.get(++idx));
        } else if(idx == items.size() - 1 && loopToggle.isSelected()){
            playMedia(items.get(0));
        }
    }
    
    private void playPrev() {
        ObservableList<MPMedia> items = playlist.getItems();
        int idx = items.indexOf(currentlyPlayingMedia);
        if(idx > 0) {
            playMedia(items.get(--idx));
        }
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
        
        Platform.runLater(() -> {
            
            // Force list cells refresh 
            playlist.refresh();
            
            // Reset progress
            timeSlider.setValue(0);
            timeSlider.setDisable(currentlyPlayingMpp.getDurationSec() == 0);
        });
        
    }
    
    private boolean removeSelectedFromPlaylist() {
        MPMedia media = playlist.getSelectionModel().getSelectedItem();
        return removeFromPlaylist(media);
    }
    
    private void setGlobalCredentials(MPMedia media) {
        if(media.getUser() != null && media.getPassword() != null) {
            Authenticator.setDefault(MPUtils.createAuthenticator(media));
        }
    }
    
    private void updatePlayTimeLabel(int seconds) {
        int hrs = (seconds / 60) / 60;
        int min = (seconds / 60) % 60;
        int sec = seconds % 60;
        playTime.setText(MessageFormat.format("{0}{1}:{2}{3}:{4}{5}", 
                hrs < 10 ? "0" : "", hrs, 
                min < 10 ? "0" : "", min, 
                sec < 10 ? "0" : "", sec));
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
