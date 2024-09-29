package mb.player.components.swing.properties;

import javax.swing.JTable;

public class PropertyTable extends JTable {
    private static final long serialVersionUID = 1L;
    
    private PropertyTableModel model;

    public PropertyTable() {
        setModel(model = new PropertyTableModel());
        getColumn("Value").setCellEditor(new PropertyTableValueCellEditor());
        getColumn("Value").setCellRenderer(new PropertyTableValueCellRenderer());
    }

    public PropertyTableModel getModel() {
        return model;
    }
    
    
    
    
    
}
