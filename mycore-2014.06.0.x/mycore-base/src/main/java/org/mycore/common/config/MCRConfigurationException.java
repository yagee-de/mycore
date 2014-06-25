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

package org.mycore.common.config;

import org.mycore.common.MCRException;

/**
 * Instances of this class represent an exception thrown because of an error in
 * the MyCoRe configuration. Normally this will be the case when a configuration
 * property that is required is not set or has an illegal value.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRConfigurationException extends MCRException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new MCRConfigurationException with an error message
     * 
     * @param message
     *            the error message for this exception
     */
    public MCRConfigurationException(String message) {
        super(message);
    }

    /**
     * Creates a new MCRConfigurationException with an error message and a
     * reference to an exception thrown by an underlying system.
     * 
     * @param message
     *            the error message for this exception
     * @param exception
     *            the exception that was thrown by an underlying system
     */
    public MCRConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}