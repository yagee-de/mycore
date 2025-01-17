/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.datamodel.ifs;

import java.util.List;

import org.mycore.common.MCRPersistenceException;

/**
 * Implementations of this class can be used to store the metadata of all
 * MCRFilesystemNodes in a persistent datastore. While MCRContentStores hold a
 * file's content, this store holds its descriptive data like the directory
 * structure, file type, checksum and size. There can only be one instance to be
 * used in a system, that instance is configured by the property
 * <b>MCR.Persistence.IFS.FileMetadataStore.Class </b>
 * 
 * @see MCRFileMetadataManager
 * @see MCRFilesystemNode
 * @see MCRContentStore
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
@Deprecated
public interface MCRFileMetadataStore {
    /**
     * Creates or updates the data of the given node in the persistent store.
     * 
     * @param node
     *            the MCRFilesystemNode to be stored
     */
    void storeNode(MCRFilesystemNode node) throws MCRPersistenceException;

    /**
     * Retrieves the MCRFilesystemNode with that ID from the persistent store.
     * 
     * @param id the
     *            unique ID of the MCRFilesystemNode
     * @return the node with that ID, or null if no such node exists
     */
    MCRFilesystemNode retrieveNode(String id) throws MCRPersistenceException;

    /**
     * Retrieves a child node of an MCRDirectory from the persistent store.
     * 
     * @param parentID
     *            the unique ID of the parent MCRDirectory
     * @param name
     *            the filename of the child node in that directory
     * @return the child MCRFilesystemNode, or null if no such node exists
     */
    MCRFilesystemNode retrieveChild(String parentID, String name) throws MCRPersistenceException;

    /**
     * Retrieves the root MCRFilesystemNode that has no parent and is owned by
     * the object with the given owner ID.
     * 
     * @param ownerID
     *            the ID of the owner of the root node.
     * @return an MCRFilesystemNode that has no parent and this owner ID, or
     *         null if no such node exists
     */
    String retrieveRootNodeID(String ownerID) throws MCRPersistenceException;

    /**
     * Returns a list of the children of a given parent MCRDirectory.
     * 
     * @param parentID
     *            the ID of the parent MCRDirectory
     * @return a List of child nodes in that directory
     */
    List<MCRFilesystemNode> retrieveChildren(String parentID) throws MCRPersistenceException;

    /**
     * Deletes all data of a given MCRFilesystemNode in the persistent metadata
     * store.
     * 
     * @param id
     *            the unique ID of the MCRFilesystemNode to delete
     */
    void deleteNode(String id) throws MCRPersistenceException;

    /**
     * Returns an object to iterate over the owner IDs.
     */
    Iterable<String> getOwnerIDs() throws MCRPersistenceException;
}
