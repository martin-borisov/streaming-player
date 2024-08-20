package mb.player.components.swing.properties;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class PropertyTableValueCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if(value instanceof Boolean) {
            value = PropertyTypeConverter.propertyToString(value);
        }
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    } 
}
