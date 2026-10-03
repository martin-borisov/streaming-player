package mb.player.components.swing;

import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;

public class ArtworkDialog extends JDialog {

    public ArtworkDialog(Frame frame, BufferedImage img, String title) {
        super(frame, false);
        setTitle(title);
        createAndLayoutComponents(img);
    }
    
    private void createAndLayoutComponents(BufferedImage img) {
        ImageIcon image1 = new ImageIcon(img);
        add(new JLabel(image1));
        pack();
    }
}
