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

package org.mycore.ocfl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceTransaction;

/**
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLPersistenceTransaction implements MCRPersistenceTransaction {

    private static final Logger LOGGER = LogManager.getLogger(MCROCFLPersistenceTransaction.class);

    // protected final MCRSession currentSession = MCRSessionMgr.getCurrentSession();
    
    // protected MCRXMLClassificationManager manager = MCRConfiguration2
    //     .getSingleInstanceOf("MCR.Classification.Manager", MCRXMLClassificationManager.class)
    //     .orElse(new MCROCFLXMLClassificationManager());

    private boolean active = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReady() {
        // TODO Auto-generated method stub
        LOGGER.debug("TRANSACTION READY CHECK");
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void begin() {
        // TODO Auto-generated method stub
        LOGGER.debug("TRANSACTION BEGIN");
        if(active){throw new IllegalStateException("TRANSACTION BEGIN");}
        active=true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void commit() {
        // TODO Auto-generated method stub
        LOGGER.debug("TRANSACTION COMMIT");
        if(!active){throw new IllegalStateException("TRANSACTION COMMIT");}
        active=false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rollback() {
        // TODO Auto-generated method stub
        LOGGER.debug("TRANSACTION ROLLBACK");
        if(!active){throw new IllegalStateException("TRANSACTION ROLLBACK");}
        active=false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getRollbackOnly() {
        // TODO Auto-generated method stub
        LOGGER.debug("TRANSACTION ROLLBACK CHECK");
        if(!active){throw new IllegalStateException("TRANSACTION ROLLBACK CHECK");}
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        // TODO Auto-generated method stub
        LOGGER.debug("TRANSACTION ACTIVE CHECK");
        return active;
    }

}
