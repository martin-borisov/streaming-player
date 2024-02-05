package mb.player.components.swing;

import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.JList;
import javax.swing.TransferHandler;

import mb.player.media.MPMedia;
import mb.player.media.MPUtils;

public class Playlist extends JList<MPMedia> {
    private static final long serialVersionUID = 1L;
    
    private PlaylistModel model;
    
    public Playlist() {
        setup();
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
        setCellRenderer(new PlaylistCellRenderer());
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

}
