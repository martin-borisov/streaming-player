package mb.player.components.swing;

import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import mb.player.media.MPMedia;
import mb.player.media.MPUtils;

public class Playlist extends JList<MPMedia> {
    private static final long serialVersionUID = 1L;
    
    private PlaylistModel model;
    private JPopupMenu popup;
    
    public Playlist() {
        setup();
        createPopupMenu();
        createListeners();
    }
    
    public void markAsPlaying(MPMedia media) {
        if(model.getAll().contains(media)) {
            ((PlaylistCellRenderer )getCellRenderer()).setCurrentlyPlayingMedia(media);
            repaint();
        }
    }
    
    private void setup() {
        setModel(model = new PlaylistModel());
        setDropMode(DropMode.ON_OR_INSERT);
        setCellRenderer(new PlaylistCellRenderer(true));
        setTransferHandler(new TransferHandler() {
            private static final long serialVersionUID = 1L;
            
            public boolean canImport(TransferSupport support) {
                boolean canImport = false;
                if(support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    support.setShowDropLocation(true);
                    support.setDropAction(TransferHandler.COPY);
                    canImport = true;
                }
                return canImport;
            }

            @SuppressWarnings({"unchecked"})
            public boolean importData(TransferSupport support) {
                boolean success = false;
                if(canImport(support)) {
                    
                    List<File> files;
                    try {
                        files = (List<File>) support.getTransferable().getTransferData(
                                DataFlavor.javaFileListFlavor);
                    } catch (Exception e) {
                        throw new RuntimeException("File drop failed", e);
                    }
                    
                    if(support.isDrop()) {
                        for (File file : files) {
                            if(MPUtils.isMedia(file)) {
                                MPMedia media = new MPMedia(file.getName(), file.toURI().toString(), MPMedia.Type.AUDIO);
                                model.addElement(media);
                                success = true;
                            }
                        }
                    } else {
                        // TODO Paste
                    }
                }
                return success;
            }
        });
    }
    
    private void createPopupMenu() {
        JMenuItem item = new JMenuItem("Remove", FontIcon.of(FontAwesomeSolid.TRASH, 15));
        item.addActionListener(e -> removeSelectedElements());
        
        popup = new JPopupMenu();
        popup.add(item);
    }
    
    private void createListeners() {
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if(e.getButton() == MouseEvent.BUTTON3) {
                    int idx = locationToIndex(e.getPoint());
                    
                    // Select artificially only if a multi select hasn't already been done
                    int[] selection = getSelectedIndices();
                    if(selection.length <= 1 || !Arrays.stream(selection).anyMatch(el -> el == idx)) {
                        setSelectedIndex(idx);
                    }
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
    
    private void removeSelectedElements() {
        model.removeElements(getSelectedValuesList());
    }
}
