package mb.player.components.swing;

import java.util.ArrayList;
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
    
    
}
