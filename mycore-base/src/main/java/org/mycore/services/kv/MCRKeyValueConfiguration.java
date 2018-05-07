package org.mycore.services.kv;

/**
 * MCR.KeyValue.Stores.EmbargoCache=org.mycore.services.kv.MCRCacheKeyValueStore
 * MCR.KeyValue.Stores.EmbargoCache.Type=EmbargoCache
 * MCR.KeyValue.Stores.EmbargoCache.Capacity=1000
 *
 * MCR.KeyValue.Extractor.ModsEmbargoExtractor=org.mycore.services.kv.MCRKVXPathExtractor
 * MCR.KeyValue.Extractor.ModsEmbargoExtractor.XPath=/mycoreobject/metadata/.../mods:embargo
 *
 * MCR.KeyValue.Configuration=%MCR.KeyValue.Configuration%;[EmbargoCache,ModsEmbargoExtractor,DEMAND]
 */
public class MCRKeyValueConfiguration {

    private MCRKeyValueExtractor extractor;
    private MCRKeyValueStore store;
    private MCRKVStrategy strategy;

    public MCRKeyValueConfiguration(MCRKeyValueExtractor extractor, MCRKeyValueStore store,
        MCRKVStrategy strategy) {
        this.extractor = extractor;
        this.store = store;
        this.strategy = strategy;
    }

    public MCRKeyValueExtractor getExtractor() {
        return extractor;
    }

    public MCRKeyValueStore getStore() {
        return store;
    }

    public MCRKVStrategy getStrategy() {
        return strategy;
    }
}
