package mb.player.components.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.apache.commons.lang3.StringUtils;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import mb.player.media.MPMedia;
import net.miginfocom.swing.MigLayout;

public class PlaylistCellRenderer extends JPanel implements ListCellRenderer<MPMedia> {
    private static final long serialVersionUID = 1L;
    
    private JLabel nameLabel, sourceLabel;
    private MPMedia currentlyPlayingMedia;
    private boolean showSource;
    
    public PlaylistCellRenderer() {
        createAndLayoutComponents();
    }
    
    public PlaylistCellRenderer(boolean showSource) {
        this();
        this.showSource = showSource;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends MPMedia> list, MPMedia media, int index,
            boolean isSelected, boolean cellHasFocus) {
        nameLabel.setIcon(media == currentlyPlayingMedia ? FontIcon.of(FontAwesomeSolid.PLAY, 15) : null);
        nameLabel.setText(media.getName());
        
        if(showSource && StringUtils.isNotBlank(media.getSource())) {
            String source = URLDecoder.decode(media.getSource(), StandardCharsets.UTF_8);
            source = StringUtils.removeStart(source, "file:");
            sourceLabel.setText(source);
        } else {
            sourceLabel.setText(StringUtils.EMPTY);
        }
        
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
    
    private void createAndLayoutComponents() {
        setLayout(new MigLayout("insets 0, gap 0, fill, wrap", "[]", "[][]"));
        
        nameLabel = new JLabel();
        nameLabel.setFont(nameLabel.getFont().deriveFont(nameLabel.getFont().getStyle() | Font.BOLD));
        add(nameLabel, "width 0::");
        sourceLabel = new JLabel();
        sourceLabel.setFont(sourceLabel.getFont().deriveFont(sourceLabel.getFont().getStyle() | Font.ITALIC));
        sourceLabel.setForeground(new Color(Integer.parseInt("778899", 16)));
        add(sourceLabel, "width 0::");
    }
}
