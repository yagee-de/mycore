package org.mycore.services.kv;

import java.util.Optional;

import org.mycore.datamodel.metadata.MCRBase;

public class MCRKVXPathExtractor extends MCRKeyValueExtractor {

    public MCRKVXPathExtractor(String id) {
        super(id);
    }

    @Override
    public Optional<String> extractValue(MCRBase base, String key) {
        final String xPath = getProperties().get("XPath");

        // Run Xpath and return value
        base.createXML();

        return Optional.empty();
    }
}
