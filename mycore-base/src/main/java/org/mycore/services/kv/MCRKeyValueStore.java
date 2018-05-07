package org.mycore.services.kv;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;

public abstract class MCRKeyValueStore {

    public static final String KEY_VALUE_STORE_PROPERTY_PREFIX = "MCR.KeyValue.Stores.";

    private final String id;

    protected MCRKeyValueStore(final String id) {
        this.id = id;
    }

    public static MCRKeyValueStore getStore(String storeID) {
        final String className = MCRConfiguration.instance().getString(KEY_VALUE_STORE_PROPERTY_PREFIX + storeID);

        try {
            @SuppressWarnings("unchecked")
            Class<MCRKeyValueStore> classObject = (Class<MCRKeyValueStore>) Class.forName(className);
            Constructor<MCRKeyValueStore> constructor = classObject.getConstructor(String.class);
            return constructor.newInstance(storeID);
        } catch (ClassNotFoundException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + (KEY_VALUE_STORE_PROPERTY_PREFIX + storeID) + ") not found: "
                    + className, e);
        } catch (NoSuchMethodException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + (KEY_VALUE_STORE_PROPERTY_PREFIX + storeID)
                    + ") needs a string constructor: " + className);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new MCRException(e);
        }
    }

    protected final Map<String, String> getProperties() {
        Map<String, String> propertiesMap = MCRConfiguration.instance()
            .getPropertiesMap(KEY_VALUE_STORE_PROPERTY_PREFIX + id + ".");

        Map<String, String> shortened = new HashMap<>();

        propertiesMap.keySet().forEach(key -> {
            String newKey = key.substring(KEY_VALUE_STORE_PROPERTY_PREFIX.length() + id.length() + 1);
            shortened.put(newKey, propertiesMap.get(key));
        });

        return shortened;
    }

    public abstract void put(final String id, final String key, final String value);

    public abstract void remove(final String id, final String key);

    public boolean has(final String id, final String key) {
        return get(id, key).isPresent();
    }

    public abstract Optional<String> get(final String id, final String key);
}
