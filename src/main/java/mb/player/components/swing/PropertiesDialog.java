package mb.player.components.swing;

import java.awt.Frame;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultRowSorter;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.RowSorter;
import javax.swing.SortOrder;

import org.apache.commons.lang3.tuple.MutablePair;

import mb.player.components.swing.properties.PropertyService;
import mb.player.components.swing.properties.PropertyTable;
import mb.player.components.swing.properties.PropertyTableModel;
import net.miginfocom.swing.MigLayout;

public class PropertiesDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    
    private PropertyTable table;
    private boolean okClicked;
    
    public PropertiesDialog(Frame frame, List<MutablePair<String, Object>> properties, boolean editable) {
        super(frame, true);
        setLocationRelativeTo(frame);
        setSize(400, 180);
        setTitle("Settings");
        createAndLayoutComponents(properties, editable);
    }

    public boolean isOkClicked() {
        return okClicked;
    }
    
    private void createAndLayoutComponents(List<MutablePair<String, Object>> properties, boolean editable) {
        setLayout(new MigLayout("wrap", "[grow]", 
                editable ? "[grow][]" : "[grow]"));
        
        table = new PropertyTable(editable);
        table.setAutoCreateRowSorter(true);
        sortedByDefault();
        add(new JScrollPane(table), "grow");
        ((PropertyTableModel)table.getModel()).setProperties(properties);
        
        if(editable) {
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
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void sortedByDefault() {
        DefaultRowSorter sorter = ((DefaultRowSorter)table.getRowSorter());
        sorter.setSortsOnUpdates(true);
        sorter.setSortKeys(Collections.singletonList(
                new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        sorter.sort();
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
        PropertiesDialog dialog = new PropertiesDialog(frame, properties, true);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
        dialog.dispose();
        
        // Store updated properties
        if(dialog.isOkClicked()) {
            PropertyService.getInstance().storeProperties(properties);
        }
    }
}
