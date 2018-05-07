package org.mycore.services.kv;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRBase;

public abstract class MCRKeyValueExtractor {

    public static final String KEY_VALUE_EXTRACTOR_PROPERTY = "MCR.KeyValue.Extractor.";

    private final String id;

    public MCRKeyValueExtractor(final String id) {
        this.id = id;
    }

    public final String getId() {
        return id;
    }

    public abstract Optional<String> extractValue(final MCRBase base, final String key);

    protected final Map<String, String> getProperties() {
        Map<String, String> propertiesMap = MCRConfiguration.instance()
            .getPropertiesMap(KEY_VALUE_EXTRACTOR_PROPERTY + id + ".");

        Map<String, String> shortened = new HashMap<>();

        propertiesMap.keySet().forEach(key -> {
            String newKey = key.substring(KEY_VALUE_EXTRACTOR_PROPERTY.length() + id.length() + 1);
            shortened.put(newKey, propertiesMap.get(key));
        });

        return shortened;
    }

}
