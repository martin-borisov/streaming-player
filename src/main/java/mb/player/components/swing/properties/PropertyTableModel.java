package mb.player.components.swing.properties;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.apache.commons.lang3.tuple.MutablePair;

public class PropertyTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMN_NAMES = new String[]{"Name", "Value"};
    
    private List<MutablePair<String, Object>> properties;

    public PropertyTableModel() {
        properties = new ArrayList<>();
        
    }

    public List<MutablePair<String, Object>> getProperties() {
        return properties;
    }

    public void setProperties(List<MutablePair<String, Object>> properties) {
        this.properties = properties;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return properties.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int i, int j) {
        Object val = null;
        MutablePair<String, Object> pair = properties.get(i);
        if(pair != null) {
            if(j == 0) {
                val = pair.getKey();
            } else if(j == 1) {
                val = pair.getValue();
            }
        }
        return val;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if(col == 1 && properties.get(row) != null) {
            properties.get(row).setRight(value);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 1;
    }
    
    
}
