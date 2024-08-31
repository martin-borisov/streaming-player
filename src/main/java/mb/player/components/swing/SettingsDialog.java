package mb.player.components.swing;

import java.awt.Frame;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import mb.player.components.swing.properties.PropertyService;
import mb.player.components.swing.properties.PropertyTable;
import mb.player.components.swing.properties.PropertyTableModel;

import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.tuple.MutablePair;

public class SettingsDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    
    private PropertyTable table;
    private boolean okClicked;
    
    public SettingsDialog(Frame frame, List<MutablePair<String, Object>> properties) {
        super(frame, true);
        setLocationRelativeTo(frame);
        setSize(400, 180);
        setTitle("Settings");
        createAndLayoutComponents(properties);
    }

    public boolean isOkClicked() {
        return okClicked;
    }
    
    private void createAndLayoutComponents(List<MutablePair<String, Object>> properties) {
        setLayout(new MigLayout("wrap", "[grow]", "[][]"));
        
        table = new PropertyTable();
        add(new JScrollPane(table), "grow");
        ((PropertyTableModel)table.getModel()).setProperties(properties);
        
        JPanel buttonsPanel = new JPanel(
                new MigLayout("insets 0, align right", "[][]", "[]"));
        add(buttonsPanel, "grow x");
        
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> {
            if(!table.isEditing()) {
                okClicked = true;
                closeDialog();
            }
        });
        buttonsPanel.add(ok);
        
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> closeDialog());
        buttonsPanel.add(cancel);
    }
    
    private void closeDialog() {
        setVisible(false);
        dispose();
    }
    
    public static void showDialog(Frame frame) {
        
        // TODO Move loading and saving properties to PropertyTableModel
        // Get stored properties and merge with default
        List<MutablePair<String, Object>> properties = 
                PropertyService.getInstance().getProperties();
        
        // Show dialog
        SettingsDialog dialog = new SettingsDialog(frame, properties);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
        dialog.dispose();
        
        // Store updated properties
        if(dialog.isOkClicked()) {
            PropertyService.getInstance().storeProperties(properties);
        }
    }
}
