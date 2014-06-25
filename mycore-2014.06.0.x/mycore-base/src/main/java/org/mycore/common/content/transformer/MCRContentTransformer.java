/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common.content.transformer;

import java.io.IOException;
import java.io.OutputStream;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;

/**
 * Subclasses of MCRContentTransformer implement different methods
 * to transform MCRContent. They may have their own additional 
 * configuration properties. Every transformer instance has a unique ID. 
 * The implementing class is configured using the property
 * 
 * MCR.ContentTransformer.{ID}.Class
 * 
 * Optionally, a transformer can set its returning MIME Type via
 * 
 * MCR.ContentTransformer.{ID}.MIMEType
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public abstract class MCRContentTransformer {

    /** The MIME type of the output generated by this transformer */
    protected String mimeType;

    protected String fileExtension;

    /** The default MIME type */
    private final static String defaultMimeType = "application/octet-stream";

    /** Called by the factory to initialize configuration of this transformer */
    public void init(String id) {
        String mimeProperty = "MCR.ContentTransformer." + id + ".MIMEType";
        String extensionProperty = "MCR.ContentTransformer." + id + ".FileExtension";
        this.mimeType = MCRConfiguration.instance().getString(mimeProperty, defaultMimeType);
        this.fileExtension = MCRConfiguration.instance().getString(extensionProperty, getDefaultExtension());
    }

    /** Transforms MCRContent. Subclasses implement different transformation methods */
    public abstract MCRContent transform(MCRContent source) throws IOException;

    public void transform(MCRContent source, OutputStream out) throws IOException {
        MCRContent content = transform(source);
        try {
            if (getEncoding() != null) {
                content.setEncoding(getEncoding());
            }
        } catch (RuntimeException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
        content.sendTo(out);
    }

    /** Returns the MIME type of the transformed content, may return the default mime type */
    public String getMimeType() throws Exception {
        return mimeType;
    }

    /**
     * Returns the encoding of characters in the binary stream.
     * 
     * Will return null if the encoding is unknown or the results does not represent character data.
     */
    public String getEncoding() throws Exception {
        return null;
    }

    /**
     * Returns the file extension that is usually related to the transformed content.
     * @return
     */
    public String getFileExtension() throws Exception {
        return fileExtension;
    }

    protected String getDefaultExtension() {
        return "bin";
    }
}