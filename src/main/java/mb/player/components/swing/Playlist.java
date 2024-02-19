package mb.player.components.swing;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DropMode;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import mb.player.media.MPMedia;
import mb.player.media.MPUtils;

public class Playlist extends JList<MPMedia> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(Playlist.class.getName());
    
    private static final DataFlavor SELECTION_DATA_HANDLER;
    static {
        try {
            SELECTION_DATA_HANDLER = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType +
                    ";class=\"" + int[].class.getName() + "\"");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    private PlaylistModel model;
    private JPopupMenu popup;
    
    public Playlist() {
        setup();
        createPopupMenu();
        createListeners();
    }
    
    public void markAsPlaying(MPMedia media) {
        
        // TODO This should happen by index not by reference
        if(model.getAll().contains(media)) {
            ((PlaylistCellRenderer)getCellRenderer()).setCurrentlyPlayingMedia(media);
            repaint();
        }
    }
    
    private void setup() {
        setBorder(new EmptyBorder(0, 4, 0, 4));
        setModel(model = new PlaylistModel());
        setDropMode(DropMode.INSERT);
        setDragEnabled(true);
        setCellRenderer(new PlaylistCellRenderer(true));
        setTransferHandler(new TransferHandler() {
            private static final long serialVersionUID = 1L;
            
            @Override
            public boolean canImport(TransferSupport support) {
                boolean canImport = false;
                if(support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    support.setShowDropLocation(true);
                    support.setDropAction(TransferHandler.COPY);
                    canImport = true;
                } else if(support.isDataFlavorSupported(SELECTION_DATA_HANDLER)) {
                    support.setShowDropLocation(true);
                    support.setDropAction(TransferHandler.MOVE);
                    canImport = true;
                }
                return canImport;
            }

            @Override
            @SuppressWarnings({"unchecked"})
            public boolean importData(TransferSupport support) {
                boolean success = false;
                if(canImport(support)) {
                    
                    if(support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        
                        // File drop
                        List<File> files = null;
                        try {
                            files = (List<File>) support.getTransferable().getTransferData(
                                    DataFlavor.javaFileListFlavor);
                        } catch (Exception e) {
                            LOG.log(Level.WARNING, "File drop failed", e);
                            return false;
                        }
                    
                        if(support.isDrop()) {
                            List<MPMedia> media = files.stream()
                                    .filter(f -> MPUtils.isMedia(f))
                                    .map(f -> new MPMedia(f.getName(), f.toURI().toString(), MPMedia.Type.AUDIO))
                                    .toList();
                            
                            JList.DropLocation loc = (JList.DropLocation) support.getDropLocation();
                            int toIdx = loc.getIndex() > -1 ? loc.getIndex() : model.getAll().size();
                            model.addElementsAt(toIdx, media);
                            success = true;
                        } else {
                            // Paste support?
                        }
                        
                    } else if(support.isDataFlavorSupported(SELECTION_DATA_HANDLER)) {
                        
                        // Playlist reorder
                        if(support.isDrop()) {
                            
                            int[] selection;
                            try {
                                selection = (int[]) support.getTransferable().getTransferData(SELECTION_DATA_HANDLER);
                            } catch (Exception e) {
                                LOG.log(Level.WARNING, "List reorder failed", e);
                                return false;
                            }
                        
                            JList.DropLocation loc = (JList.DropLocation) support.getDropLocation();
                            int toIdx = loc.getIndex();
                            if(toIdx > -1) {
                                
                                LOG.fine("Selection Indices: " + Arrays.toString(selection) + ", Drop Index: [" + toIdx + "]");
                                
                                // Count selection indexes smaller than drop index
                                long count = Arrays.stream(selection).filter(i -> i < toIdx).count();
                                LOG.fine("Index count before drop: " + count);
                                
                                // Remove first
                                List<MPMedia> removed = model.removeElements(selection);
                                
                                // Then re-add
                                LOG.fine("Recalculated drop index: [" + (toIdx - count) + "]");
                                model.addElementsAt(toIdx - (int)count, removed);
                                success = true;
                            }
                        }
                    }
                }
                return success;
            }
        });
        new PlaylistDragSourceListener(this);
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
                    
                    // Select artificially only if a multiselect hasn't already been done
                    int[] selection = getSelectedIndices();
                    if(selection.length <= 1 || !Arrays.stream(selection).anyMatch(el -> el == idx)) {
                        setSelectedIndex(idx);
                    }
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                case KeyEvent.VK_DELETE:
                case KeyEvent.VK_BACK_SPACE:
                    removeSelectedElements();
                    break;
                }
            }
        });
    }
    
    public void removeSelectedElements() {
        model.removeElements(getSelectedIndices());
    }
    
    /* Drag Implementation */
    
    private class PlaylistDragSourceListener implements DragSourceListener, DragGestureListener {
        
        private Playlist playlist;
        private DragSource ds;
        
        public PlaylistDragSourceListener(Playlist playlist) {
            this.playlist = playlist;
            ds = new DragSource();
            ds.createDefaultDragGestureRecognizer(playlist,
                    DnDConstants.ACTION_MOVE, this);
        }

        @Override
        public void dragGestureRecognized(DragGestureEvent dge) {
            int[] selection = playlist.getSelectedIndices();
            if(selection.length > 0) {
                ds.startDrag(dge, DragSource.DefaultMoveDrop, 
                        new SelectionTransferable(selection), this);
            }
        }

        @Override
        public void dragEnter(DragSourceDragEvent dsde) {
        }

        @Override
        public void dragOver(DragSourceDragEvent dsde) {
        }

        @Override
        public void dropActionChanged(DragSourceDragEvent dsde) {
        }

        @Override
        public void dragExit(DragSourceEvent dse) {
        }
        
        @Override
        public void dragDropEnd(DragSourceDropEvent dsde) {
            LOG.fine("DnD Success: " + dsde.getDropSuccess());
        }
    }
    
    private class SelectionTransferable implements Transferable {
        
        private int[] selection;
        
        public SelectionTransferable(int[] selection) {
            this.selection = selection;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{SELECTION_DATA_HANDLER};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return SELECTION_DATA_HANDLER.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            return selection;
        }
        
    }
    
}
