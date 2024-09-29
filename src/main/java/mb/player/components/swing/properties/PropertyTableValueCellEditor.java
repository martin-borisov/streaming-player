package mb.player.components.swing.properties;

import java.awt.Component;
import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import org.apache.commons.lang3.BooleanUtils;

public class PropertyTableValueCellEditor extends AbstractCellEditor implements TableCellEditor {
    private static final long serialVersionUID = 1L;
    
    private JTextField textField;
    private JComboBox<String> comboBox;
    private Component pickedComponent;

    public PropertyTableValueCellEditor() {
        createComponents();
    }

    @Override
    public Object getCellEditorValue() {
        return pickedComponent == textField ? textField.getText() : 
                PropertyTypeConverter.stringToProperty((String) comboBox.getSelectedItem());
    }

    @Override
    public Component getTableCellEditorComponent(JTable jtable, Object value, boolean isSelected, int row, int col) {
        Component component;
        if(value instanceof Boolean) {
            comboBox.setSelectedItem(BooleanUtils.toStringYesNo((boolean) value));
            component = comboBox;
        } else {
            textField.setText(String.valueOf(value));
            component = textField;
        }
        return pickedComponent = component;
    }
    
    private void createComponents() {
        textField = new JTextField();
        comboBox = new JComboBox<>(new String[]{"yes", "no"});
    }
}
