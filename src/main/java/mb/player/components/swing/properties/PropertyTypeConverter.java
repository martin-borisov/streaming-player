package mb.player.components.swing.properties;

import org.apache.commons.lang3.BooleanUtils;

public class PropertyTypeConverter {
    
    public static Object stringToProperty(String str) {
        Object prop = str;
        Boolean bool = BooleanUtils.toBooleanObject(str);
        if(bool != null) {
            prop = bool;
        }
        return prop;
    }
    
    public static String propertyToString(Object prop) {
        String str;
        if(prop instanceof Boolean) {
            str = BooleanUtils.toStringYesNo((Boolean) prop);
        } else {
            str = String.valueOf(prop);
        }
        return str;
    }
    
}
