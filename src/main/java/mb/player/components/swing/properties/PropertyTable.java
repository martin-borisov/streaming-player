package mb.player.components.swing.properties;

import javax.swing.JTable;

public class PropertyTable extends JTable {
    private static final long serialVersionUID = 1L;
    
    private PropertyTableModel model;

    public PropertyTable(boolean editable) {
        setModel(model = new PropertyTableModel(editable));
        getColumn("Value").setCellRenderer(new PropertyTableValueCellRenderer());
        
        if(editable) {
            getColumn("Value").setCellEditor(new PropertyTableValueCellEditor());
        }
    }

    public PropertyTableModel getModel() {
        return model;
    }
    
    
    
    
    
}
