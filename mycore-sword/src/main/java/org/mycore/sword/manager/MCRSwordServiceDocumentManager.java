package org.mycore.sword.manager;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.sword.MCRSword;
import org.mycore.sword.MCRSwordConstants;
import org.mycore.sword.application.MCRSwordCollectionProvider;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.ServiceDocument;
import org.swordapp.server.ServiceDocumentManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordCollection;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.SwordWorkspace;

/**
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRSwordServiceDocumentManager implements ServiceDocumentManager {

    @Override
    public ServiceDocument getServiceDocument(String sdUri, AuthCredentials auth, SwordConfiguration config)
        throws SwordError, SwordServerException, SwordAuthException {
        ServiceDocument serviceDocument = new ServiceDocument();

        MCRSword.getWorkspaces().forEach(workspaceName -> {
            SwordWorkspace workspace = buildSwordWorkspace(workspaceName, auth);
            serviceDocument.addWorkspace(workspace);
        });

        return serviceDocument;
    }

    private SwordWorkspace buildSwordWorkspace(String workspaceName, AuthCredentials auth) {
        SwordWorkspace workspace = new SwordWorkspace();
        workspace.setTitle(workspaceName);

        buildSwordCollectionList(workspaceName, auth).forEach(workspace::addCollection);

        return workspace;
    }

    private List<SwordCollection> buildSwordCollectionList(String workspaceName, AuthCredentials auth) {
        String baseURL = MCRFrontendUtil.getBaseURL();
        List<SwordCollection> swordCollections = new ArrayList<>();

        MCRSword.getCollectionsOfWorkspace(workspaceName).stream()
            .map(collection -> new AbstractMap.SimpleEntry<>(collection, MCRSword.getCollection(collection)))
            .filter(collectionEntry -> collectionEntry.getValue().isVisible())
            .forEach(collection -> {
                SwordCollection swordCollection = new SwordCollection();
                final String collectionTitle = collection.getKey();
                swordCollection.setTitle(collectionTitle);

                // add the supported packaging to the collection Provider
                final MCRSwordCollectionProvider collectionProvider = collection.getValue();
                collectionProvider.getSupportedPagacking().forEach(swordCollection::addAcceptPackaging);

                swordCollection.setHref(baseURL + MCRSwordConstants.SWORD2_COL_IRI + collectionTitle + "/");
                swordCollections.add(swordCollection);
            });

        return swordCollections;
    }
}
