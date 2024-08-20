package mb.player.components.swing.properties;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.stream.Collectors;
import mb.player.components.swing.ConfigService;
import org.apache.commons.lang3.event.EventListenerSupport;
import org.apache.commons.lang3.tuple.MutablePair;


public class PropertyService {
    
    private static PropertyService ref;
    private EventListenerSupport<PropertyChangeListener> listenerSupport = 
            EventListenerSupport.create(PropertyChangeListener.class);

    public static PropertyService getInstance() {
        synchronized (PropertyService.class) {
            if(ref == null) {
                ref = new PropertyService();
            }
        }
        return ref;
    }

    private PropertyService() {
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        listenerSupport.addListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        listenerSupport.removeListener(listener);
    }
    
    public List<MutablePair<String, Object>> getProperties() {
        ConfigService config = ConfigService.getInstance();
        return config.getPropertyNames().stream().
                map(n -> new MutablePair<>(n, PropertyTypeConverter.stringToProperty(config.getProperty(n)))).
                collect(Collectors.toList());
    }
    
    public MutablePair<String, Object> getOrCreateProperty(String key, Object defaultValue) {
        String value = ConfigService.getInstance().getOrCreateProperty(key, 
                PropertyTypeConverter.propertyToString(defaultValue));
        return new MutablePair<>(key, PropertyTypeConverter.stringToProperty(value));
    }
    
    public void setProperty(MutablePair<String, Object> property) {
        ConfigService.getInstance().setProperty(property.getLeft(),
                    PropertyTypeConverter.propertyToString(property.getRight()));
        listenerSupport.fire().propertyChange(
                    new PropertyChangeEvent(this, property.getLeft(), null, property.getRight()));
    }
    
    public void storeProperties(List<MutablePair<String, Object>> properties) {
        properties.stream().forEach(p -> {
            ConfigService.getInstance().setProperty(p.getLeft(),
                    PropertyTypeConverter.propertyToString(p.getRight()));
            listenerSupport.fire().propertyChange(
                    new PropertyChangeEvent(this, p.getLeft(), null, p.getRight()));
        });
    }
}
