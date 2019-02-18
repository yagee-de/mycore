package org.mycore.common.content.transformer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.common.xsl.MCRParameterCollector;

public class MCRStaticContentTransformer extends MCRParameterizedTransformer {

    private boolean ignoreSessiondata;

    private Path staticFilePath;

    private String id;

    @Override
    public void init(String id) {
        this.id = id;
        super.init(id);

        this.ignoreSessiondata = MCRConfiguration2.getBoolean("MCR.ContentTransformer." + id + ".IgnoreSessionData")
            .orElse(false);

        this.staticFilePath = MCRConfiguration2.getString("MCR.ContentTransformer." + id + ".StaticStorePath")
            .map(Paths::get)
            .orElseGet(() -> Paths.get(MCRConfiguration2.getStringOrThrow("MCR.datadir"), "static/"));

        if (!Files.exists(this.staticFilePath)) {
            try {
                Files.createDirectories(this.staticFilePath);
            } catch (IOException e) {
                throw new MCRException("Error while creating static content Folder!", e);
            }
        }

    }

    @Override
    public MCRContent transform(MCRContent source) throws IOException {
        return transform(source, (MCRParameterCollector) null);
    }

    private String getStaticContentID(MCRContent source) throws IOException {
        return id + "_" + Optional.ofNullable(source.getSystemId())
            .orElse(source.getName()).replaceAll("/", "_")
            .replaceAll("[\\\\]", "_").replaceAll(":", "_");
    }

    private void storeStaticContent(MCRContent content, String sourceID) {
        try {
            Files.copy(content.getInputStream(), getStaticContentPath(sourceID),
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new MCRException("Error while storing content!", e);
        }
    }

    private Optional<MCRPathContent> getStoredContent(String contentID) {
        final Path staticContentPath = getStaticContentPath(contentID);

        if (Files.exists(staticContentPath)) {
            return Optional.of(new MCRPathContent(staticContentPath));
        }
        return Optional.empty();
    }

    private Path getStaticContentPath(String contentID) {
        return this.staticFilePath.resolve(contentID);
    }

    private long getSuppressedLastModified(MCRContent c) {
        try {
            return c.lastModified();
        } catch (IOException e) {
            return -1;
        }
    }

    private long getStaticLastModified(String contentID) {
        return getStoredContent(contentID).map(this::getSuppressedLastModified).orElse((long) -1);
    }

    private MCRContent doTransform(MCRContent source, MCRParameterCollector parameter) throws IOException {
        MCRContentTransformer originalTransformer = getOriginalTransformer();
        if (parameter != null && originalTransformer instanceof MCRParameterizedTransformer) {
            return ((MCRParameterizedTransformer) originalTransformer).transform(source, parameter);
        } else {
            return originalTransformer.transform(source);
        }
    }

    private MCRContentTransformer getOriginalTransformer() {
        return MCRConfiguration2.getString("MCR.ContentTransformer." + id + ".Transformer")
            .map(MCRContentTransformerFactory::getTransformer)
            .orElseThrow(() -> new MCRConfigurationException(
                "The property MCR.ContentTransformer." + id + ".Transformer is not set!"));
    }

    @Override
    public MCRContent transform(MCRContent source, MCRParameterCollector parameter) throws IOException {
        final String sourceID = getStaticContentID(source);
        if ((source.getSystemId() == null && source.getName() == null) || (source.isUsingSession() && !ignoreSessiondata)) {
            return doTransform(source, parameter);
        }

        if (source.lastModified() > getStaticLastModified(sourceID)) {
            final MCRContent result;
            result = doTransform(source, parameter);
            storeStaticContent(result, sourceID);
        }

        return getStoredContent(sourceID).get();
    }

}
