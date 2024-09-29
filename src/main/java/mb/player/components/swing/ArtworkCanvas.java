package mb.player.components.swing;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class ArtworkCanvas extends Canvas {
    private static final long serialVersionUID = 1L;
    
    private BufferedImage image;
    
    public void paint(Graphics g) {
        if (image != null) {
            int sizeFactor = Math.round((float) image.getHeight() / (float) image.getWidth());
            if (getHeight() > getWidth()) {
                int scaledHeight = getWidth() * sizeFactor;
                g.drawImage(image, 0, getHeight() / 2 - scaledHeight / 2, getWidth(), scaledHeight, null);
            } else {
                int scaledWidth = getHeight() * sizeFactor;
                g.drawImage(image, getWidth() / 2 - scaledWidth / 2, 0, scaledWidth, getHeight(), null);
            }
        } else {
            g.clearRect(0, 0, getWidth(), getHeight());
        }
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }
}
