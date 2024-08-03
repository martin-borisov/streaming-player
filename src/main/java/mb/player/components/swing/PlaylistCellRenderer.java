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
import mb.player.media.MPUtils;
import net.miginfocom.swing.MigLayout;

public class PlaylistCellRenderer extends JPanel implements ListCellRenderer<MPMedia> {
    private static final long serialVersionUID = 1L;
    private static final Color DETAILS_COLOR = new Color(Integer.parseInt("778899", 16));
    
    private JLabel nameLabel, sourceLabel, attribsLabel;
    private MPMedia currentlyPlayingMedia;
    private boolean showDetails;
    
    public PlaylistCellRenderer() {
        createAndLayoutComponents();
    }
    
    public PlaylistCellRenderer(boolean showDetails) {
        this();
        this.showDetails = showDetails;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends MPMedia> list, MPMedia media, int index,
            boolean isSelected, boolean cellHasFocus) {
        nameLabel.setIcon(media == currentlyPlayingMedia ? FontIcon.of(FontAwesomeSolid.PLAY, 15) : null);
        nameLabel.setText(media.getName());
        String source = URLDecoder.decode(StringUtils.removeStart(media.getSource(), "file:"), 
                StandardCharsets.UTF_8);
        
        if(showDetails) {
            sourceLabel.setText(source);
            sourceLabel.setVisible(true);
            attribsLabel.setText(media.getDurationSec() > 0 ? 
                    MPUtils.secToTimeFormat(media.getDurationSec()) : "N/A");
            attribsLabel.setVisible(true);
        } else {
            sourceLabel.setText(StringUtils.EMPTY);
            sourceLabel.setVisible(false);
            attribsLabel.setText(StringUtils.EMPTY);
            attribsLabel.setVisible(false);
        }
        
        StringBuilder tooltipBuf = new StringBuilder();
        tooltipBuf.append(media.getName()).append("\n").append(source);
        if(media.getType() != null) {
            tooltipBuf.append("\n").append("Type: ").append(media.getType());
        }
        tooltipBuf.append("\n").append("Length: ").append(media.getDurationSec() > 0 ? 
                    MPUtils.secToTimeFormat(media.getDurationSec()) : "N/A");
        setToolTipText(tooltipBuf.toString());
        
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

    public boolean isShowDetails() {
        return showDetails;
    }

    public void setShowDetails(boolean showDetails) {
        this.showDetails = showDetails;
    }
    
    private void createAndLayoutComponents() {
        setLayout(new MigLayout("insets 0, gap 0, fill, wrap", "[]", "[][]"));
        
        nameLabel = new JLabel();
        nameLabel.setFont(nameLabel.getFont().deriveFont(nameLabel.getFont().getStyle() | Font.BOLD));
        add(nameLabel, "width 0::");
        sourceLabel = new JLabel();
        sourceLabel.setFont(sourceLabel.getFont().deriveFont(sourceLabel.getFont().getStyle() | Font.ITALIC));
        sourceLabel.setForeground(DETAILS_COLOR);
        add(sourceLabel, "width 0::");
        attribsLabel = new JLabel();
        attribsLabel.setFont(sourceLabel.getFont().deriveFont(sourceLabel.getFont().getStyle() | Font.ITALIC));
        attribsLabel.setForeground(DETAILS_COLOR);
        add(attribsLabel, "width 0::");
    }
}
