/*
 * $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.services.zipper;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Deflater;

import javax.servlet.ServletOutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;

/**
 * Uses ZIP format to deliver requested content.
 * {@link Deflater#BEST_COMPRESSION} is used for compression.
 * @author Thomas Scheffler
 */
public class MCRZipServlet extends MCRCompressServlet<ZipArchiveOutputStream> {
    private static final long serialVersionUID = 1L;

    @Override
    protected void sendCompressed(MCRDirectory dir, ZipArchiveOutputStream container) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(dir.getPath() + "/");
        entry.setComment(dir.getLabel());
        entry.setTime(dir.getLastModified().getTimeInMillis());
        container.putArchiveEntry(entry);
        container.closeArchiveEntry();
        super.sendCompressed(dir, container);
    }

    @Override
    protected void sendCompressed(MCRFile file, ZipArchiveOutputStream container) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(file.getPath());
        entry.setTime(file.getLastModified().getTimeInMillis());
        entry.setComment(file.getLabel());
        entry.setSize(file.getSize());
        container.putArchiveEntry(entry);
        try (InputStream in = file.getContentAsInputStream()) {
            IOUtils.copy(in, container);
        } finally {
            container.closeArchiveEntry();
        }
    }

    @Override
    protected void sendMetadataCompressed(String fileName, byte[] content, long lastModified,
        ZipArchiveOutputStream container) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(fileName);
        entry.setSize(content.length);
        entry.setTime(lastModified);
        container.putArchiveEntry(entry);
        container.write(content);
        container.closeArchiveEntry();
    }

    @Override
    protected String getMimeType() {
        return "application/zip";
    }

    @Override
    protected String getFileExtension() {
        return "zip";
    }

    @Override
    protected ZipArchiveOutputStream createContainer(ServletOutputStream sout, String comment) {
        ZipArchiveOutputStream zout = new ZipArchiveOutputStream(new BufferedOutputStream(sout));
        zout.setComment(comment);
        zout.setLevel(Deflater.BEST_COMPRESSION);
        return zout;
    }

    @Override
    protected void disposeContainer(ZipArchiveOutputStream container) throws IOException {
        container.finish();
    }
}