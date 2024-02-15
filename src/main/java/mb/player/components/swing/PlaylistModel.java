package mb.player.components.swing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;

import mb.player.media.MPMedia;

public class PlaylistModel extends AbstractListModel<MPMedia> {
    private static final long serialVersionUID = 1L;
    
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
}
