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

package org.mycore.frontend.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.comparator.NameFileComparator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.hibernate.tables.MCRFSNODES;
import org.mycore.backend.hibernate.tables.MCRFSNODES_;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRContentStore;
import org.mycore.datamodel.ifs.MCRContentStoreFactory;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.ifs2.MCRCStoreIFS2;
import org.mycore.datamodel.ifs2.MCRFileCollection;
import org.mycore.datamodel.ifs2.MCRNode;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Attributes2Impl;

@MCRCommandGroup(name = "IFS Maintenance Commands")
@Deprecated
public class MCRIFSCommands {
    public static final String MCRFILESYSTEMNODE_SIZE_FIELD_NAME = "size";

    public static final String MCRFILESYSTEMNODE_TOUCH_METHOD_NAME = "touch";

    private static final String ELEMENT_FILE = "file";

    private static final String CDATA = "CDATA";

    private static final String ATT_FILE_NAME = "name";

    private static final String NS_URI = "";

    private static Logger LOGGER = LogManager.getLogger(MCRIFSCommands.class);

    private static int MAX_COUNTER = 10000;

    @MCRCommand(syntax = "generate md5sum files in directory {0}",
        help = "writes md5sum files for every content store in directory {0}")
    public static void writeMD5SumFile(String targetDirectory) throws IOException {
        File targetDir = getDirectory(targetDirectory);
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<MCRFSNODES> streamQuery = em
            .createQuery("from MCRFSNODES where type='F' order by storeid, storageid", MCRFSNODES.class);
        Map<String, MCRContentStore> availableStores = MCRContentStoreFactory.getAvailableStores();
        String currentStoreId = null;
        MCRContentStore currentStore = null;
        File currentStoreBaseDir = null;
        BufferedWriter bw = null;
        String nameOfProject = MCRConfiguration2.getString("MCR.NameOfProject").orElse("MyCoRe");
        try {
            Iterator<MCRFSNODES> fsnodes = streamQuery.getResultStream().iterator();
            while (fsnodes.hasNext()) {
                MCRFSNODES fsNode = fsnodes.next();
                String storeID = fsNode.getStoreid();
                String storageID = fsNode.getStorageid();
                String md5 = fsNode.getMd5();
                em.detach(fsNode);
                if (!storeID.equals(currentStoreId)) {
                    //initialize current store
                    currentStoreId = storeID;
                    currentStore = availableStores.get(storeID);
                    if (bw != null) {
                        bw.close();
                    }
                    File outputFile = new File(targetDir,
                        new MessageFormat("{0}-{1}.md5", Locale.ROOT).format(new Object[] { nameOfProject, storeID }));
                    LOGGER.info("Writing to file: {}", outputFile.getAbsolutePath());
                    bw = Files.newBufferedWriter(outputFile.toPath(), Charset.defaultCharset(),
                        StandardOpenOption.CREATE);
                    try {
                        currentStoreBaseDir = currentStore.getBaseDir();
                    } catch (Exception e) {
                        LOGGER.warn("Could not get baseDir of store: {}", storeID, e);
                        currentStoreBaseDir = null;
                    }
                }
                String path = currentStoreBaseDir != null ? currentStore.getLocalFile(storageID).getAbsolutePath()
                    : storageID;
                //current store initialized
                String line = new MessageFormat("{0}  {1}\n", Locale.ROOT).format(new Object[] { md5, path });
                bw.write(line);
            }
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e1) {
                    LOGGER.warn("Error while closing file.", e1);
                }
            }
            em.clear();
        }
    }

    @MCRCommand(syntax = "generate missing file report in directory {0}",
        help = "generates XML a report over all content stores about missing files and write it in directory {0}")
    public static void writeMissingFileReport(String targetDirectory) throws IOException, SAXException,
        TransformerConfigurationException {
        File targetDir = getDirectory(targetDirectory);
        FSNodeChecker checker = new LocalFileExistChecker();
        writeReport(targetDir, checker);
    }

    @MCRCommand(syntax = "generate md5 file report in directory {0}",
        help = "generates XML a report over all content stores about failed md5 checks and write it in directory {0}")
    public static void writeFileMD5Report(String targetDirectory) throws IOException, SAXException,
        TransformerConfigurationException {
        File targetDir = getDirectory(targetDirectory);
        FSNodeChecker checker = new MD5Checker();
        writeReport(targetDir, checker);
    }

    private static void writeReport(File targetDir, FSNodeChecker checker) throws TransformerFactoryConfigurationError,
        SAXException, IOException, TransformerConfigurationException {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<MCRFSNODES> query = em.createQuery("from MCRFSNODES where type='F' order by storeid, owner, name",
            MCRFSNODES.class);
        Map<String, MCRContentStore> availableStores = MCRContentStoreFactory.getAvailableStores();
        String currentStoreId = null;
        MCRContentStore currentStore = null;
        File currentStoreBaseDir = null;
        StreamResult streamResult = null;
        String nameOfProject = MCRConfiguration2.getString("MCR.NameOfProject").orElse("MyCoRe");
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler th = null;
        Attributes2Impl atts = new Attributes2Impl();
        final String rootName = checker.getName();
        final String elementName = ELEMENT_FILE;
        final String attBasedir = "basedir";
        final String nsURI = NS_URI;
        final String attType = CDATA;
        String owner = null;

        try (Stream<MCRFSNODES> resultStream = query.getResultStream()) {
            Iterator<MCRFSNODES> fsnodes = resultStream.iterator();
            while (fsnodes.hasNext()) {
                MCRFSNODES fsNode = fsnodes.next();
                String storeID = fsNode.getStoreid();
                String storageID = fsNode.getStorageid();
                em.detach(fsNode);
                if (!storeID.equals(currentStoreId)) {
                    //initialize current store
                    currentStoreId = storeID;
                    currentStore = availableStores.get(storeID);
                    if (th != null) {
                        th.endElement(nsURI, rootName, rootName);
                        th.endDocument();
                        OutputStream outputStream = streamResult.getOutputStream();
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    }
                    File outputFile = new File(targetDir, new MessageFormat("{0}-{1}-{2}.xml", Locale.ROOT)
                        .format(new Object[] { nameOfProject, storeID, rootName }));
                    streamResult = new StreamResult(new FileOutputStream(outputFile));
                    th = tf.newTransformerHandler();
                    Transformer serializer = th.getTransformer();
                    serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                    serializer.setOutputProperty(OutputKeys.INDENT, "yes");
                    th.setResult(streamResult);
                    LOGGER.info("Writing to file: {}", outputFile.getAbsolutePath());
                    th.startDocument();
                    atts.clear();
                    atts.addAttribute(nsURI, "project", "project", attType, nameOfProject);
                    try {
                        currentStoreBaseDir = currentStore.getBaseDir();
                        atts.addAttribute(nsURI, attBasedir, attBasedir, attType,
                            currentStoreBaseDir.getAbsolutePath());
                    } catch (Exception e) {
                        LOGGER.warn("Could not get baseDir of store: {}", storeID, e);
                        currentStoreBaseDir = null;
                    }
                    th.startElement(nsURI, rootName, rootName, atts);
                }
                if (!fsNode.getOwner().equals(owner)) {
                    owner = fsNode.getOwner();
                    LOGGER.info("Checking owner/derivate: {}", owner);
                }

                File f = null;
                try {
                    f = currentStore.getLocalFile(storageID);
                } catch (IOException e) {
                    LOGGER.warn("Missing file with storageID: {}", storageID);
                }
                if (!checker.checkNode(fsNode, f, atts)) {
                    th.startElement(nsURI, elementName, elementName, atts);
                    th.endElement(nsURI, elementName, elementName);
                }
            }
        } finally {
            em.clear();
            if (th != null) {
                try {
                    th.endElement(nsURI, rootName, rootName);
                    th.endDocument();
                    OutputStream outputStream = streamResult.getOutputStream();
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e1) {
                    LOGGER.warn("Error while closing file.", e1);
                }
            }
        }
    }

    static File getDirectory(String targetDirectory) {
        File targetDir = new File(targetDirectory);
        if (!targetDir.isDirectory()) {
            throw new IllegalArgumentException("Target directory " + targetDir.getAbsolutePath()
                + " is not a directory.");
        }
        return targetDir;
    }

    @MCRCommand(syntax = "generate missing nodes report in directory {0}",
        help = "generates XML report over all content stores about missing ifs nodes and write it in directory {0}")
    public static void writeMissingNodesReport(String targetDirectory) throws SAXException,
        TransformerConfigurationException, IOException {
        File targetDir = getDirectory(targetDirectory);
        Map<String, MCRContentStore> availableStores = MCRContentStoreFactory.getAvailableStores();
        final String nsURI = NS_URI;
        final String attType = CDATA;
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        Attributes2Impl atts = new Attributes2Impl();
        final String rootName = "missingnodes";
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        for (MCRContentStore currentStore : availableStores.values()) {
            File baseDir;
            try {
                baseDir = currentStore.getBaseDir();
                if (baseDir == null) {
                    LOGGER.warn("Could not get baseDir of store: {}", currentStore.getID());
                    continue;
                }
            } catch (Exception e) {
                LOGGER.warn("Could not get baseDir of store: {}", currentStore.getID(), e);
                continue;
            }
            TypedQuery<String> streamQuery = em
                .createQuery("select storeageid from MCRFSNODES where type='F' and storeid=:storeid order by storageid",
                    String.class)
                .setParameter("storeid", currentStore.getID());

            boolean endOfList = false;
            String nameOfProject = MCRConfiguration2.getString("MCR.NameOfProject").orElse("MyCoRe");
            String storeID = currentStore.getID();
            File outputFile = new File(targetDir, new MessageFormat("{0}-{1}-{2}.xml", Locale.ROOT)
                .format(new Object[] { nameOfProject, storeID, rootName }));
            StreamResult streamResult;
            try {
                streamResult = new StreamResult(new FileOutputStream(outputFile));
            } catch (FileNotFoundException e) {
                //should not happen as we checked it before
                LOGGER.error(e);
                return;
            }
            try {
                TransformerHandler th = tf.newTransformerHandler();
                Transformer serializer = th.getTransformer();
                serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                serializer.setOutputProperty(OutputKeys.INDENT, "yes");
                th.setResult(streamResult);
                LOGGER.info("Writing to file: {}", outputFile.getAbsolutePath());
                th.startDocument();
                atts.clear();
                atts.addAttribute(nsURI, "project", "project", attType, nameOfProject);
                atts.addAttribute(nsURI, "store", "store", attType, storeID);
                atts.addAttribute(nsURI, "baseDir", "baseDir", attType, baseDir.getAbsolutePath());
                th.startElement(nsURI, rootName, rootName, atts);
                URI baseURI = baseDir.toURI();
                try (Stream<String> resultStream = streamQuery.getResultStream()) {
                    Iterator<String> storageIds = resultStream.iterator();
                    int rows = -1;
                    for (File currentFile : new FileStoreIterator(baseDir)) {
                        if (currentFile.isDirectory()) {
                            String relative = baseURI.relativize(currentFile.toURI()).getPath();
                            LOGGER.info("Checking segment: {}", relative);
                        } else {
                            int checkFile = endOfList ? -1 : checkFile(baseURI, currentFile, storageIds, rows);
                            rows++;
                            endOfList = checkFile == -1;
                            if (endOfList || checkFile == 1) {
                                LOGGER.warn("Found orphaned file: {}", currentFile);
                                atts.clear();
                                atts.addAttribute(NS_URI, ATT_FILE_NAME, ATT_FILE_NAME, CDATA,
                                    baseURI.relativize(currentFile.toURI()).getPath());
                                th.startElement(NS_URI, ELEMENT_FILE, ELEMENT_FILE, atts);
                                th.endElement(NS_URI, ELEMENT_FILE, ELEMENT_FILE);
                            }
                        }
                    }
                }
                th.endElement(nsURI, rootName, rootName);
                th.endDocument();
            } finally {
                OutputStream stream = streamResult.getOutputStream();
                if (stream != null) {
                    stream.close();
                }
            }
        }
    }

    @MCRCommand(syntax = "delete ifs node {0}", help = "deletes ifs node {0} recursivly")
    public static void deleteIFSNode(String nodeID) {
        MCRFilesystemNode node = MCRFilesystemNode.getNode(nodeID);
        if (node == null) {
            LOGGER.warn("IFS Node {} does not exist.", nodeID);
            return;
        }
        LOGGER.info("Deleting IFS Node {}: {}{}", nodeID, node.getOwnerID(), node.getAbsolutePath());
        node.delete();
    }

    @MCRCommand(syntax = "repair directory sizes of derivate {0}", help = "Fixes the directory sizes of a derivate.")
    public static void fixDirectorysOfDerivate(String id) {
        MCRDirectory mcrDirectory = (MCRDirectory) MCRFilesystemNode.getRootNode(id);

        if (mcrDirectory == null) {
            throw new IllegalArgumentException(String.format(Locale.ENGLISH, "Could not get root node for %s.", id));
        }

        fixDirectorySize(mcrDirectory);
    }

    private static long fixDirectorySize(MCRDirectory directory) {
        long directorySize = 0;

        for (MCRFilesystemNode child : directory.getChildren()) {
            if (child instanceof MCRDirectory) {
                directorySize += fixDirectorySize((MCRDirectory) child);
            } else if (child instanceof MCRFile) {
                MCRFile file = (MCRFile) child;
                directorySize += file.getSize();
            }
        }
        /*
            There is no setSize method on MCRFileSystemNode and there should not be one.
            But in this repair command we need to set the size, so we use reflection.
         */
        try {
            Field privateLongField = MCRFilesystemNode.class.getDeclaredField(MCRFILESYSTEMNODE_SIZE_FIELD_NAME);
            privateLongField.setAccessible(true);
            privateLongField.set(directory, directorySize);

        } catch (NoSuchFieldException e) {
            String message = String.format(Locale.ENGLISH, "There is no field named %s in MCRFileSystemNode!",
                MCRFILESYSTEMNODE_SIZE_FIELD_NAME);
            throw new MCRException(message, e);
        } catch (IllegalAccessException e) {
            String message = new MessageFormat("Could not acces filed {0} in {1}!", Locale.ROOT)
                .format(new Object[] { MCRFILESYSTEMNODE_SIZE_FIELD_NAME, directory.toString() });
            throw new MCRException(message, e);
        }

        // now call touch with the old date of MCRFSN to apply the changes to the DB
        GregorianCalendar lastModified = directory.getLastModified();
        FileTime lastModifiedFileTime = FileTime.fromMillis(lastModified.getTimeInMillis());

        try {
            Method touchMethod = MCRFilesystemNode.class.getDeclaredMethod(MCRFILESYSTEMNODE_TOUCH_METHOD_NAME,
                FileTime.class, boolean.class);
            touchMethod.setAccessible(true);
            touchMethod.invoke(directory, lastModifiedFileTime, false);
        } catch (NoSuchMethodException e) {
            throw new MCRException(
                String.format(Locale.ENGLISH, "There is no %s-method..", MCRFILESYSTEMNODE_TOUCH_METHOD_NAME));
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new MCRException(
                String.format(Locale.ENGLISH, "Error while calling %s-method..", MCRFILESYSTEMNODE_TOUCH_METHOD_NAME));
        }

        LOGGER.info("Changed size of directory {} to {} Bytes", directory.getName(), directorySize);
        return directorySize;
    }

    /**
     *
     * @param baseURI
     * @param currentFile
     * @param storageIds
     * @return 0 (node present), 1 (node not present), -1 (end of storageIds)
     */
    private static int checkFile(URI baseURI, File currentFile, Iterator<String> storageIds, int count) {
        if (count == -1) {
            //go to first Result;
            if (!storageIds.hasNext()) {
                return 1;
            }
        }
        String storageId = storageIds.next();
        String relativePath = baseURI.relativize(currentFile.toURI()).getPath();
        int comp = relativePath.compareTo(storageId);
        while (comp > 0) {
            if (storageIds.hasNext()) {
                storageId = storageIds.next();
                comp = relativePath.compareTo(storageId);
            } else {
                return -1;
            }
        }
        return comp == 0 ? 0 : 1;
    }

    @MCRCommand(syntax = "move derivates from content store {0} to content store {1} for owner {2}",
        help = "moves all files of derivates from content store {0} to content store {1} for defined owner {2}")
    public static List<String> moveContentOfOwnerToNewStore(String sourceStore, String targetStore, String owner) {
        LOGGER.info("Start move data from content store {} to store {} for owner {}", sourceStore, targetStore,
            owner);
        return moveContentToNewStore(sourceStore, targetStore, "owner", owner);
    }

    @MCRCommand(syntax = "move derivates from content store {0} to content store {1} for filetype {2}",
        help = "moves all files of derivates from content store {0} to content store {1} for defined file type {2} - "
            + "delimiting number of moved files with property MCR.IFS.ContentStore.MoveCounter")
    public static List<String> moveContentOfFiletypeToNewStore(String sourceStore, String targetStore,
        String fileType) {
        LOGGER.info("Start move data from content store {} to store {} for file type {}", sourceStore, targetStore,
            fileType);
        return moveContentToNewStore(sourceStore, targetStore, "fctid", fileType);
    }

    private static List<String> moveContentToNewStore(String sourceStore, String targetStore, String selectKey,
        String selectValue) {
        // check stores
        MCRContentStore fromStore = MCRContentStoreFactory.getStore(sourceStore);
        @SuppressWarnings("unused")
        MCRContentStore toStore = MCRContentStoreFactory.getStore(targetStore);
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();

        TypedQuery<String> streamQuery = em
            .createQuery(
                "SELECT id from MCRFSNODES where storeid=:storeid and " + selectKey + "=:selectValue order by owner",
                String.class)
            .setParameter("storeid", fromStore.getID())
            .setParameter("selectValue", selectValue);
        try (Stream<String> resultStream = streamQuery.getResultStream()) {
            return resultStream
                .map(ifsId -> String.format(Locale.ROOT, "move ifs node %s to store %s", ifsId, targetStore))
                .collect(Collectors.toList());
        }
    }

    @MCRCommand(syntax = "move ifs node {0} to store {1}",
        help = "Moves the MCRFile with IFSID {0} to a new MCRContentStore with ID {1}")
    public static void moveFile(String ifsId, String storeID) throws IOException {
        MCRContentStore store = MCRContentStoreFactory.getStore(storeID);
        String storageID = moveFile(ifsId, store);
        LOGGER.debug("File id={} has storage ID {} in store {}.", ifsId, storageID, store);
    }

    private static String moveFile(String ifsId, MCRContentStore target) throws IOException {
        MCRFile sourceFile = MCRFile.getFile(Objects.requireNonNull(ifsId));
        sourceFile.moveTo(target);
        return sourceFile.getStorageID();
    }

    @MCRCommand(syntax = "check derivates of mcrfsnodes with project id {0}",
        help = "check the entries of MCRFSNODES for all derivates with project ID {0}")
    public static void checkMCRFSNODESForDerivatesWithProjectID(String projectId) {
        LOGGER.info("Start check of MCRFSNODES for derivates with project ID {}", projectId);
        if (projectId == null || projectId.length() == 0) {
            LOGGER.error("Project ID missed for check MCRFSNODES entries of derivates with project ID {0}");
            return;
        }
        Map<String, MCRContentStore> availableStores = MCRContentStoreFactory.getAvailableStores();
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        MCRXMLMetadataManager mgr = MCRXMLMetadataManager.instance();
        List<String> idList = mgr.listIDsForBase(projectId + "_derivate");
        int counter = 0;
        int maxresults = idList.size();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRFSNODES> query = cb.createQuery(MCRFSNODES.class);
        Root<MCRFSNODES> nodes = query.from(MCRFSNODES.class);
        ParameterExpression<String> ownerID = cb.parameter(String.class);
        TypedQuery<MCRFSNODES> typedQuery = em
            .createQuery(query
                .where(
                    cb.equal(nodes.get(MCRFSNODES_.owner), ownerID),
                    cb.equal(nodes.get(MCRFSNODES_.type), "F"))
                .orderBy(cb.asc(nodes.get(MCRFSNODES_.storageid))));
        for (String derid : idList) {
            counter++;
            LOGGER.info("Processing dataset {} from {} with ID: {}", counter, maxresults, derid);
            // check mcrfsnodes entries
            try {
                AtomicInteger nodeCount = new AtomicInteger();
                typedQuery.setParameter(ownerID, derid);
                typedQuery
                    .getResultList()
                    .stream()
                    .forEach(fsNode -> {
                        nodeCount.incrementAndGet();
                        String storeName = fsNode.getStoreid();
                        String storageid = fsNode.getStorageid();
                        String name = fsNode.getName();
                        long size = fsNode.getSize();
                        Date date = fsNode.getDate();
                        GregorianCalendar datecal = new GregorianCalendar(TimeZone.getDefault(), Locale.getDefault());
                        datecal.setTime(date);
                        String fctid = fsNode.getFctid();
                        String md5 = fsNode.getMd5();
                        em.detach(fsNode);
                        LOGGER.debug(
                            "File for [owner] {} [name] {} [storeid] {} [storageid] {} [fctid] {} [size] {} [md5] {}",
                            derid, name, storeName, storageid, fctid, size, md5);
                        // get path of file
                        MCRContentStore fsStore = availableStores.get(storeName);
                        if (fsStore == null) {
                            LOGGER.error("Can't find content store {}", storeName);
                            return;
                        }
                        try {
                            File contentFile = fsStore.getLocalFile(storageid);
                            if (contentFile == null || !contentFile.canRead()) {
                                LOGGER.error("   !!!! Can't access to file {} of store {}", storageid, storeName);
                            }
                        } catch (Exception e) {
                            LOGGER.error("   !!!! Can't access to file {} of store {}", storageid, storeName);
                        }
                    });
                if (nodeCount.get() == 0) {
                    LOGGER.error("   !!!! Can't find file entries in MCRFSNODES for {}", derid);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                em.clear();
            }
        }
        LOGGER.info("Check done for {} entries", Integer.toString(counter));
    }

    @MCRCommand(syntax = "check mcrfsnodes of derivates with project id {0}",
        help = "check the entries of MCRFSNODES with project ID {0} that the derivate exists")
    public static void checkDerivatesWithProjectIDInMCRFSNODES(String projectId) {
        LOGGER.info("Start check of MCRFSNODES for derivates with project ID {}", projectId);
        if (projectId == null || projectId.length() == 0) {
            LOGGER.error("Project ID missed for check MCRFSNODES entries of derivates with project ID {0}");
            return;
        }
        MCRXMLMetadataManager mgr = MCRXMLMetadataManager.instance();
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<MCRFSNODES> nodes = query.from(MCRFSNODES.class);
        AtomicInteger counter = new AtomicInteger();
        em.createQuery(query
            .distinct(true)
            .select(nodes.get(MCRFSNODES_.owner))
            .where(cb.like(nodes.get(MCRFSNODES_.owner), projectId + "\\_%")))
            .getResultList()
            .stream()
            .peek(ignore -> counter.incrementAndGet())
            .map(MCRObjectID::getInstance)
            .filter(derID -> {
                try {
                    return !mgr.exists(derID);
                } catch (MCRPersistenceException e) {
                    LOGGER.error("Error while checking existence of {}", derID, e);
                    return true;
                }
            })
            .forEach(missingDerivate -> LOGGER.error("   !!!! Can't find MCRFSNODES entry {} as existing derivate",
                missingDerivate));
        LOGGER.info("Check done for {} entries", counter.get());
    }

    @MCRCommand(syntax = "check IFS2 compatibility", help = "checks if content is compatible with MyCoRe 2019 LTS")
    public static void checkBeforeUpgrade() {
        AtomicBoolean result = new AtomicBoolean(true);
        List<String> usedStores = getUsedStores();
        if (usedStores.size() > 1) {
            LOGGER.error("MyCoRe LTS 2019 does not support multiple content stores. "
                + "Please move your content from store(s) {} to store IFS2.",
                usedStores.stream().filter(s -> !"IFS2".equals(s)).collect(Collectors.joining(", ")));
            result.set(false);
        }
        usedStores
            .stream()
            .filter(storeId -> !(MCRContentStoreFactory.getStore(storeId) instanceof MCRCStoreIFS2))
            .forEach(storeId -> {
                LOGGER.error("Content store {} is not an instance of {}. Please move content.", storeId,
                    MCRCStoreIFS2.class);
                result.set(false);
            });
        usedStores
            .stream()
            .map(MCRContentStoreFactory::getStore)
            .filter(MCRCStoreIFS2.class::isInstance)
            .forEach(store -> checkStore(result, (MCRCStoreIFS2) store));
        if (result.get()) {
            LOGGER.info("Your content is ready for MyCoRe LTS 2019");
        } else {
            LOGGER.error("Your content is not yet ready for MyCoRe LTS 2019. "
                + "Please correct the errors and run this command again.");
        }

    }

    @MCRCommand(syntax = "update IFS2 MD5 sums", help = "copies verified IFS1 MD5 sums to IFS2")
    public static void copyMD5ToIFS2() {
        AtomicBoolean result = new AtomicBoolean(true);
        List<String> usedStores = getUsedStores();
        usedStores
            .stream()
            .map(MCRContentStoreFactory::getStore)
            .filter(MCRCStoreIFS2.class::isInstance)
            .forEach(store -> copyMD5ToIFS2(result, (MCRCStoreIFS2) store));
        if (!result.get()) {
            throw new MCRException("Could not copy all MD5 information to IFS2. Please see error messages above.");
        }
    }

    private static void copyMD5ToIFS2(AtomicBoolean result, MCRCStoreIFS2 storeIFS2) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRFSNODES> query = cb.createQuery(MCRFSNODES.class);
        Root<MCRFSNODES> root = query.from(MCRFSNODES.class);
        (em.createQuery(query
            .where(cb.equal(root.get(MCRFSNODES_.storeid), storeIFS2.getID()),
                cb.equal(root.get(MCRFSNODES_.type), "F"))
            .orderBy(cb.asc(root.get(MCRFSNODES_.owner)), cb.asc(cb.length(root.get(MCRFSNODES_.storageid))))))
                .getResultStream()
                .peek(em::detach)
                .filter(f -> MCRObjectID.isValid(f.getOwner()))
                .map(f -> {
                    try {
                        return new AbstractMap.SimpleEntry<>(f.getMd5(),
                            toFile(storeIFS2, f));
                    } catch (IOException e) {
                        LOGGER.error("Could not get information from ifs node {}", f.getStorageid(), e);
                        result.set(false);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(e -> {
                    if (!e.getKey().equals(e.getValue().getMD5())) {
                        String path = e.getValue().getLocalPath().toAbsolutePath().toString();
                        LOGGER.info("Update MD5 sum for file {} to {}",
                            path, e.getKey());
                        try {
                            e.getValue().setMD5(e.getKey());
                        } catch (IOException e1) {
                            LOGGER.error("Could not update MD5 sum of " + path, e);
                            result.set(false);
                        }
                    }
                });
    }

    private static List<String> getUsedStores() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<MCRFSNODES> root = query.from(MCRFSNODES.class);
        return em.createQuery(query
            .distinct(true)
            .select(root.get(MCRFSNODES_.storeid))
            .where(cb.equal(root.get(MCRFSNODES_.type), "F"))).getResultList();

    }

    private static void checkStore(AtomicBoolean result, MCRCStoreIFS2 storeIFS2) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRFSNODES> query = cb.createQuery(MCRFSNODES.class);
        Root<MCRFSNODES> root = query.from(MCRFSNODES.class);
        (em.createQuery(query
            .where(cb.equal(root.get(MCRFSNODES_.storeid), storeIFS2.getID()),
                cb.equal(root.get(MCRFSNODES_.type), "F"))
            .orderBy(cb.asc(root.get(MCRFSNODES_.owner)), cb.asc(cb.length(root.get(MCRFSNODES_.storageid))))))
                .getResultStream()
                .peek(em::detach)
                .filter(f -> MCRObjectID.isValid(f.getOwner()))
                .map(f -> {
                    try {
                        return new AbstractMap.SimpleEntry<>(f.getMd5(),
                            toFile(storeIFS2, f));
                    } catch (IOException e) {
                        LOGGER.error("Could not get information from ifs node {}", f.getStorageid(), e);
                        result.set(false);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(e -> {
                    if (!e.getKey().equals(e.getValue().getMD5())) {
                        String path = e.getValue().getLocalPath().toAbsolutePath().toString();
                        LOGGER.error("MD5 sum mismatch for file {}. DB:{}, mcrdata.xml:{}",
                            path, e.getKey(), e.getValue().getMD5());
                        result.set(false);
                    }
                });
    }

    private static org.mycore.datamodel.ifs2.MCRFile toFile(MCRCStoreIFS2 store, MCRFSNODES node) throws IOException {
        MCRFileCollection derivateRoot = store.getIFS2FileCollection(MCRObjectID.getInstance(node.getOwner()));
        String storageid = node.getStorageid();
        if (node.getSize() == 0) {
            //no storageId in IFS1
            storageid = MCRFile.getFile(node.getId()).getPath();
        }
        String path = toPath(storageid);
        MCRNode nodeByPath = derivateRoot.getNodeByPath(path);
        if (nodeByPath == null) {
            throw new FileNotFoundException(node.getOwner() + ":" + path);
        }
        if (nodeByPath instanceof org.mycore.datamodel.ifs2.MCRFile) {
            return (org.mycore.datamodel.ifs2.MCRFile) nodeByPath;
        }
        throw new IOException(
            node.getName() + " is not a file (" + node.getOwner() + ":" + path + "): "
                + nodeByPath.getClass().getName());
    }

    private static String toPath(String storageID) {
        int pos = storageID.indexOf("/") + 1;
        return storageID.substring(pos);
    }

    private abstract static class FSNodeChecker {
        static final String ATT_STORAGEID = "storageid";

        static final String ATT_OWNER = "owner";

        static final String ATT_NAME = "fileName";

        static final String ATT_MD5 = "md5";

        static final String ATT_SIZE = "size";

        static final String ATT_IFS_ID = "ifsid";

        public abstract String getName();

        public abstract boolean checkNode(MCRFSNODES node, File localFile, Attributes2Impl atts);

        void addBaseAttributes(MCRFSNODES node, Attributes2Impl atts) {
            atts.clear();
            atts.addAttribute(NS_URI, ATT_SIZE, ATT_SIZE, CDATA, Long.toString(node.getSize()));
            atts.addAttribute(NS_URI, ATT_MD5, ATT_MD5, CDATA, node.getMd5());
            atts.addAttribute(NS_URI, ATT_STORAGEID, ATT_STORAGEID, CDATA, node.getStorageid());
            atts.addAttribute(NS_URI, ATT_OWNER, ATT_OWNER, CDATA, node.getOwner());
            atts.addAttribute(NS_URI, ATT_NAME, ATT_NAME, CDATA, node.getName());
            atts.addAttribute(NS_URI, ATT_IFS_ID, ATT_IFS_ID, CDATA, node.getId());
        }

    }

    private static class LocalFileExistChecker extends FSNodeChecker {
        @Override
        public String getName() {
            return "missing";
        }

        @Override
        public boolean checkNode(MCRFSNODES node, File localFile, Attributes2Impl atts) {
            if (localFile != null && localFile.exists()) {
                return true;
            }
            LOGGER.warn("File is missing: {}", localFile);
            addBaseAttributes(node, atts);
            return false;
        }

    }

    private static final class MD5Checker extends LocalFileExistChecker {
        @Override
        public String getName() {
            return "md5";
        }

        @Override
        public boolean checkNode(MCRFSNODES node, File localFile, Attributes2Impl atts) {
            if (!super.checkNode(node, localFile, atts)) {
                atts.addAttribute(MCRIFSCommands.NS_URI, super.getName(), super.getName(), MCRIFSCommands.CDATA,
                    "true");
                return false;
            }
            addBaseAttributes(node, atts);
            if (localFile.length() != node.getSize()) {
                LOGGER.warn("File size does not match for file: {}", localFile);
                atts.addAttribute(MCRIFSCommands.NS_URI, "actualSize", "actualSize", MCRIFSCommands.CDATA,
                    Long.toString(localFile.length()));
                return false;
            }
            //we can check MD5Sum
            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(localFile);
            } catch (FileNotFoundException e1) {
                //should not happen as we check it before
                LOGGER.warn(e1);
                return false;
            }
            String md5Sum;
            try {
                md5Sum = MCRUtils.getMD5Sum(fileInputStream);
            } catch (IOException e) {
                LOGGER.error(e);
                return false;
            }
            if (md5Sum.equals(node.getMd5())) {
                return true;
            }
            LOGGER.warn("MD5 sum does not match for file: {}", localFile);
            atts.addAttribute(MCRIFSCommands.NS_URI, "actualMD5", "actualMD5", MCRIFSCommands.CDATA, md5Sum);
            return false;
        }
    }

    public static class FileStoreIterator implements Iterable<File> {

        private File baseDir;

        public FileStoreIterator(File basedir) throws NotDirectoryException {
            if (!basedir.isDirectory()) {
                throw new NotDirectoryException(basedir.toString());
            }
            this.baseDir = basedir;
        }

        @Override
        public Iterator<File> iterator() {
            return new Iterator<>() {
                File currentDir = baseDir;

                LinkedList<File> files = getInitialList(currentDir);

                LinkedList<Iterator<File>> iterators = initIterator();

                @Override
                public boolean hasNext() {
                    if (iterators.isEmpty()) {
                        return false;
                    }
                    if (!iterators.getFirst().hasNext()) {
                        iterators.removeFirst();
                        return hasNext();
                    }
                    return true;
                }

                private LinkedList<Iterator<File>> initIterator() {
                    LinkedList<Iterator<File>> iterators = new LinkedList<>();
                    iterators.add(getIterator(files));
                    return iterators;
                }

                private Iterator<File> getIterator(LinkedList<File> files) {
                    return files.iterator();
                }

                private LinkedList<File> getInitialList(File currentDir) {
                    File[] children = currentDir.listFiles();
                    Arrays.sort(children, NameFileComparator.NAME_COMPARATOR);
                    LinkedList<File> list = new LinkedList<>();
                    list.addAll(Arrays.asList(children));
                    return list;
                }

                @Override
                public File next() {
                    if (iterators.isEmpty()) {
                        throw new NoSuchElementException("No more files");
                    }
                    File next = iterators.getFirst().next();
                    if (next.isDirectory()) {
                        LinkedList<File> list = getInitialList(next);
                        if (!list.isEmpty()) {
                            Iterator<File> iterator = getIterator(list);
                            iterators.addFirst(iterator);
                        }
                    }
                    return next;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("remove() is not supported");
                }
            };
        }
    }

}
