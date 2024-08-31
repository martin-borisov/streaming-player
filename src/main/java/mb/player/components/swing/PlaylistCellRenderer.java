package mb.player.components.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import mb.player.components.swing.properties.PropertyService;

import org.apache.commons.lang3.StringUtils;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import mb.player.media.MPMedia;
import mb.player.media.MPUtils;
import net.miginfocom.swing.MigLayout;

public class PlaylistCellRenderer extends JPanel implements ListCellRenderer<MPMedia> {
    private static final long serialVersionUID = 1L;
    private static final Color DETAILS_FONT_COLOR = new Color(Integer.parseInt("778899", 16));
    private static final float DETAILS_FONT_SIZE_FACTOR = 0.9f;
    private static final int ICON_SIZE = 40;
    private static final Icon DEFAULT_ICON = 
            FontIcon.of(FontAwesomeSolid.QUESTION, ICON_SIZE);
    
    private JLabel imageLabel, nameLabel, sourceLabel, attribsLabel;
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
        
        // Title
        StringBuilder title = new StringBuilder(
                media.getTitle() != null ? media.getTitle() : media.getName());
        if(media.getDurationSec() > 0) {
            title.append(" (").append(MPUtils.secToTimeFormatNoHours(media.getDurationSec())).append(")");
        }

        nameLabel.setText(title.toString());
        String source = URLDecoder.decode(StringUtils.removeStart(media.getSource(), "file:"), 
                StandardCharsets.UTF_8);
        
        if((boolean) PropertyService.getInstance().getOrCreateProperty(
                PropertyNamesConst.SHOW_MEDIA_ATTRIBUTES_PROPERTY_NAME, true).getValue()) {
            sourceLabel.setText(source);
            sourceLabel.setVisible(true);
            
            StringBuilder attribsBuf = new StringBuilder();
            attribsBuf.append(media.getArtist() != null ? media.getArtist() : "<unknown>").append(" | ")
                    .append(media.getAlbum() != null ? media.getAlbum() : "<unknown>");
            attribsLabel.setText(attribsBuf.toString());
            attribsLabel.setVisible(true);
            
            Icon icon = DEFAULT_ICON; 
            if(media.getArtworkThumb() != null) {
                icon = new ImageIcon(media.getArtworkThumb());
            }
            imageLabel.setIcon(icon);
            imageLabel.setVisible(true);
        } else {
            imageLabel.setVisible(false);
            sourceLabel.setVisible(false);
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
        setLayout(new MigLayout("insets 0, gap 0, hidemode 2", "[]5[]", "[][][]"));
        
        imageLabel = new JLabel();
        add(imageLabel, "spany 3");
        
        nameLabel = new JLabel();
        nameLabel.setFont(nameLabel.getFont().deriveFont(nameLabel.getFont().getStyle() | Font.BOLD));
        add(nameLabel, "width 0::, wrap");
        
        sourceLabel = new JLabel();
        sourceLabel.setFont(sourceLabel.getFont().deriveFont(sourceLabel.getFont().getStyle() | Font.ITALIC, 
                sourceLabel.getFont().getSize() * DETAILS_FONT_SIZE_FACTOR));
        sourceLabel.setForeground(DETAILS_FONT_COLOR);
        add(sourceLabel, "width 0::, wrap");
        
        attribsLabel = new JLabel();
        attribsLabel.setFont(sourceLabel.getFont());
        attribsLabel.setForeground(DETAILS_FONT_COLOR);
        add(attribsLabel, "width 0::, wrap");
    }
}
