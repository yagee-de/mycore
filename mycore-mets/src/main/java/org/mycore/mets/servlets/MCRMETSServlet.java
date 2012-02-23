/*
 * $Id$ $Revision:
 * 20489 $ $Date$
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

package org.mycore.mets.servlets;

import java.text.MessageFormat;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.mets.model.MCRMETSGenerator;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRMETSServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRMETSServlet.class);

    private boolean useExpire;

    private static int CACHE_TIME;

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.frontend.servlets.MCRServlet#init()
     */
    @Override
    public void init() throws ServletException {
        super.init();
        String cacheParam = getInitParameter("cacheTime");
        /* default is one day */
        CACHE_TIME = cacheParam != null ? Integer.parseInt(cacheParam) : (60 * 60 * 24);
        useExpire = MCRConfiguration.instance().getBoolean("MCR.Component.MetsMods.Servlet.UseExpire", true);
    }

    private boolean useExistingMets(HttpServletRequest request) {
        String useExistingMetsParam = request.getParameter("useExistingMets");
        if (useExistingMetsParam == null)
            return true;
        return Boolean.valueOf(useExistingMetsParam);
    }

    protected static String getOwnerID(String pathInfo) {
        StringBuilder ownerID = new StringBuilder(pathInfo.length());
        boolean running = true;
        for (int i = (pathInfo.charAt(0) == '/') ? 1 : 0; (i < pathInfo.length() && running); i++) {
            switch (pathInfo.charAt(i)) {
            case '/':
                running = false;
                break;
            default:
                ownerID.append(pathInfo.charAt(i));
                break;
            }
        }
        return ownerID.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.mycore.frontend.servlets.MCRServlet#getLastModified(javax.servlet
     * .http.HttpServletRequest)
     */
    @Override
    protected long getLastModified(HttpServletRequest request) {
        String ownerID = getOwnerID(request.getPathInfo());
        MCRSession session = MCRSessionMgr.getCurrentSession();
        try {
            session.beginTransaction();
            MCRDirectory rootNode = MCRDirectory.getRootDirectory(ownerID);
            if (rootNode != null)
                return rootNode.getLastModified().getTimeInMillis();
            return -1l;
        } finally {
            session.commitTransaction();
            MCRSessionMgr.releaseCurrentSession();
            session.close(); // just created session for db transaction
        }
    }

}
