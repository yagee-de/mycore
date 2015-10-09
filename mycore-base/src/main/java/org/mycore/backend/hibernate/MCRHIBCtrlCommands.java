/*
 * 
 * $Revision$ $Date$
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

package org.mycore.backend.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

/**
 * This class provides a set of commands for the org.mycore.access package which
 * can be used by the command line interface. (creates sql tables, run queries
 * 
 * @author Thomas Scheffler (yagee)
 * @author Arne Seifert
 */
@MCRCommandGroup(name = "MCR Hibernate Control Commands")
public class MCRHIBCtrlCommands extends MCRAbstractCommands {
    /** The logger */
    private static final Logger LOGGER = Logger.getLogger(MCRHIBCtrlCommands.class.getName());

    /**
     * 
     * method creates tables using hibernate
     */
    @MCRCommand(syntax = "init hibernate", help = "Creates all database tables for MyCoRe used by hibernate",
            order = 10)
    public static void createTables() {
        try {
            new SchemaUpdate(MCRHIBConnection.instance().getConfiguration()).execute(true, true);
            LOGGER.info("tables created.");
        } catch (MCRPersistenceException e) {
            throw new MCRException("error while creating tables.", e);
        } catch (HibernateException e) {
            throw new MCRException("Hibernate error while creating database tables.", e);
        }
    }
}