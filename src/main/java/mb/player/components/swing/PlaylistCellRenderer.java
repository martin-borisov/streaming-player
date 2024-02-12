package mb.player.components.swing;

import java.awt.Component;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.apache.commons.lang3.StringUtils;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import mb.player.media.MPMedia;

public class PlaylistCellRenderer extends JLabel implements ListCellRenderer<MPMedia> {
    private static final long serialVersionUID = 1L;
    
    private MPMedia currentlyPlayingMedia;
    private boolean showSource;
    
    public PlaylistCellRenderer() {
        setOpaque(true);
    }
    
    public PlaylistCellRenderer(boolean showSource) {
        this();
        this.showSource = showSource;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends MPMedia> list, MPMedia media, int index,
            boolean isSelected, boolean cellHasFocus) {
        
        StringBuilder buf = new StringBuilder();
        buf.append("<html><body>");
        buf.append("<p><b>").append(media.getName()).append("</b></p>");
        
        if(showSource && StringUtils.isNotBlank(media.getSource())) {
            String source = URLDecoder.decode(media.getSource(), StandardCharsets.UTF_8);
            source = StringUtils.removeStart(source, "file:");
            source = StringUtils.abbreviate(source, 100);
            buf.append("<p><i style=\"color: #778899;\">").append(source).append("</i></p>");
        }
        buf.append("</body></html>");
        
        setIcon(media == currentlyPlayingMedia ? FontIcon.of(FontAwesomeSolid.PLAY, 15) : null);
        setText(buf.toString());
        
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        return this;
    }

    public void setCurrentlyPlayingMedia(MPMedia currentlyPlayingMedia) {
        this.currentlyPlayingMedia = currentlyPlayingMedia;
    }

    public boolean isShowSource() {
        return showSource;
    }

    public void setShowSource(boolean showSource) {
        this.showSource = showSource;
    }
}
