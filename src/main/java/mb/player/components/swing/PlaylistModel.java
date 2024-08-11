package mb.player.components.swing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractListModel;
import javax.swing.SwingWorker;

import mb.player.media.MPMedia;
import mb.player.media.MediaPreProcessor;

public class PlaylistModel extends AbstractListModel<MPMedia> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(PlaylistModel.class.getName());
    
    private List<MPMedia> mediaList;
    
    public PlaylistModel() {
        mediaList = new ArrayList<MPMedia>();
    }

    @Override
    public int getSize() {
        return mediaList.size();
    }

    @Override
    public MPMedia getElementAt(int index) {
        return mediaList.get(index);
    }
    
    public void addElement(MPMedia media) {
        int idx = mediaList.size();
        mediaList.add(media);
        fireIntervalAdded(this, idx, idx);
    }
    
    public void addElement(int index, MPMedia media) {
        mediaList.add(index, media);
        fireIntervalAdded(this, index, index);
    }
    
    public void addElementsAt(int index, List<MPMedia> media) {
        mediaList.addAll(index, media);
        fireIntervalAdded(this, index, index + media.size());
    }

    public void addAll(Collection<? extends MPMedia> c) {
        if(c != null && !c.isEmpty()) {
            int idx = mediaList.size();
            mediaList.addAll(c);
            fireIntervalAdded(this, idx, mediaList.size() - 1);
            
            // Enrich media with additional attributes and fire update
            enrichMedia(c, idx, mediaList.size() - 1);
        }
    }
    
    public List<MPMedia> getAll() {
        return Collections.unmodifiableList(mediaList);
    }
    
    public List<MPMedia> removeElements(int[] indices) {
        List<MPMedia> removed = new ArrayList<>();
        
        // Reverse sort the indices before removing elements
        Arrays.stream(indices)
            .boxed()
            .sorted((i1 , i2) -> i1 < i2 ? 1 : (i1 > i2 ? -1 : 0))
            .forEach(i -> { 
                removed.add(mediaList.remove(i.intValue()));
                fireIntervalRemoved(this, i, i);
            });
        Collections.reverse(removed);
        return removed;
    }
    
    private void enrichMedia(Collection<? extends MPMedia> media, int idx0, int idx1) { 
        new SwingWorker<String, MPMedia>() {
            
            @Override
            protected String doInBackground() throws Exception {
                media.stream().forEach(m -> {
                    MediaPreProcessor mpp = new MediaPreProcessor(m);
                    m.setDurationSec(mpp.getDurationSec());
                    m.setTitle((String) mpp.getAttributes().get("title"));
                    m.setArtist((String) mpp.getAttributes().get("artist"));
                    m.setAlbum((String) mpp.getAttributes().get("album"));
                    
                    // TODO Set additional attributes
                    
                    publish(m);
                });
                return "MPMedia list pre-processing done";
            }

            @Override
            protected void process(List<MPMedia> chunks) {
                LOG.log(Level.FINE, "Pre-processed {0} MPMedia instances", chunks.size());
            }

            @Override
            protected void done() {
                try {
                    LOG.log(Level.FINE, get());
                    fireContentsChanged(this, idx0, idx1);
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, null, e);
                }
            }
        }.execute();
    }
}
