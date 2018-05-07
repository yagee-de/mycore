package org.mycore.services.kv;

import java.util.Map;
import java.util.Optional;

import org.mycore.common.MCRCache;

public class MCRCacheKeyValueStore extends MCRKeyValueStore {

    public static final String CAPACITY_PROPERTY = "Capacity";

    public static final String TYPE_PROPERTY = "TYPE";

    private MCRCache<String, String> cache;

    public MCRCacheKeyValueStore(final String id) {
        super(id);

        final Map<String, String> properties = getProperties();
        final String capacityString = properties.getOrDefault(CAPACITY_PROPERTY, "1000");
        final long capacity = Long.parseLong(capacityString);
        final String type = properties.getOrDefault(TYPE_PROPERTY, "KV-" + id);

        cache = new MCRCache<>(capacity, type);
    }

    private String buildInternKey(String id, String key) {
        return id + ":" + key;
    }

    @Override
    public void put(String id, String key, String value) {
        cache.put(buildInternKey(id, key), value);
    }


    @Override
    public void remove(String id, String key) {
        cache.remove(buildInternKey(id, key));
    }

    @Override
    public Optional<String> get(String id, String key) {
        return Optional.ofNullable(cache.get(buildInternKey(id, key)));
    }

}
