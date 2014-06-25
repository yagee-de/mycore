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
package org.mycore.access;

import java.util.Collection;

import org.jdom2.Element;
import org.mycore.common.MCRException;

/**
 * This serves as an interface to an underlying access controll system.
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 1.3
 */
public interface MCRAccessInterface {

    /**
     * create an access rule in the rulestore using an rule string in plain text
     * 
     * @param rule
     *              the rule string in plain text
     * @param creator
     * @param description
     *              a String description of the rule in prosa 
     */
    public void createRule(String rule, String creator, String description);

    /**
     * create an access rule in the rulestore using an rule string in plain text
     * 
     * @param rule
     *              the rule string as xml
     * @param creator
     * @param description
     *              a String description of the rule in prosa 
     */
    public void createRule(Element rule, String creator, String description);

    /**
     * generate rule string from xml
     * 
     * @param rule
     * @return the normalized rule string
     */
    public String getNormalizedRuleString(Element rule);

    /**
     * adds an access rule for an ID to an access system. The parameter
     * <code>id</code> serves as an identifier for the concrete underlying
     * rule, e.g. a MCRObjectID.
     * 
     * @param id
     *            the ID-String of the object
     * @param permission
     *            the access permission for the rule
     * @param rule
     *            the access rule
     * @param description
     *            a String description of the rule in prosa            
     * @throws MCRException
     *             if an error occured
     */
    public void addRule(String id, String permission, org.jdom2.Element rule, String description) throws MCRException;

    /**
     * adds an access rule for an "a priori-permission" like "create-document"
     * 
     * @param permission
     *            the access permission for the rule (e.g. "create-document")
     * @param rule
     *            the access rule
     * @param description
     *            a String description of the rule in prosa            
     * @throws MCRException
     *             if an error occured
     */
    public void addRule(String permission, org.jdom2.Element rule, String description) throws MCRException;

    /**
     * removes a rule. The parameter <code>id</code> serves as an identifier
     * for the concrete underlying rule, e.g. a MCRObjectID.
     * 
     * @param id
     *            the ID-String of the object
     * @param permission
     *            the access permission for the rule
     * @throws MCRException
     *             if an error occured
     */
    public void removeRule(String id, String permission) throws MCRException;

    /**
     * removes a rule for an "a priori permission" like "create-document"
     * 
     * @param permission
     *            the access permission for the rule
     * @throws MCRException
     *             if an error occured
     */
    public void removeRule(String permission) throws MCRException;

    /**
     * removes all rules of the <code>id</code>. The parameter
     * <code>id</code> serves as an identifier for the concrete underlying
     * rule, e.g. a MCRObjectID.
     * 
     * @param id
     *            the ID-String of the object
     * @throws MCRException
     *             if an errow was occured
     */
    public void removeAllRules(String id) throws MCRException;

    /**
     * updates an access rule for an ID to an access system. The parameter
     * <code>id</code> serves as an identifier for the concrete underlying
     * rule, e.g. a MCRObjectID.
     * 
     * @param id
     *            the ID-String of the object
     * @param permission
     *            the access permission for the rule
     * @param rule
     *            the access rule
     * @param description
     *            a String description of the rule in prosa 
     * @throws MCRException
     *             if an errow was occured
     */
    public void updateRule(String id, String permission, Element rule, String description) throws MCRException;

    /**
     * updates an access rule for an "a priori permission" 
     * of an access system like "create-document".
     * 
     * @param permission
     *            the access permission for the rule
     * @param rule
     *            the access rule
     * @param description
     *            a String description of the rule in prosa 
     * @throws MCRException
     *             if an errow was occured
     */
    public void updateRule(String permission, Element rule, String description) throws MCRException;

    /**
     * determines whether the current user has the permission to perform a
     * certain action.
     * same as:
     * <pre>
     *  {@link MCRAccessRule} rule={@link #getAccessRule(String, String)};
     *  if (rule==null)
     *      return false;
     *  return rule.validate();
     * </pre>
     */
    public boolean checkPermission(String id, String permission);
    
    /**
     * returns a MCRAccessRule which could be validated
     * 
     * All information regarding the current user is capsulated by a
     * <code>MCRSession</code> instance which can be retrieved by
     * 
     * <pre>
     * MCRSession currentSession = MCRSessionMgr.getCurrentSession();
     * </pre>
     * 
     * The parameter <code>id</code> serves as an identifier for the concrete
     * underlying rule, e.g. a MCRObjectID.
     * 
     * @param id
     *            the ID-String of the object
     * @param permission
     *            the permission/action to be granted, e.g. "read"
     * @return MCRAccessRule instance or null if no rule is defined;
     * @see org.mycore.common.MCRSessionMgr#getCurrentSession()
     * @see org.mycore.common.MCRSession
     */
    public MCRAccessRule getAccessRule(String id, String permission);

    /**
     * determines whether a given user has the permission to perform a
     * certain action. no session data will be checked here.
     * 
     * 
     * The parameter <code>id</code> serves as an identifier for the concrete
     * underlying rule, e.g. a MCRObjectID.
     * 
     * @param id
     *            the ID-String of the object
     * @param permission
     *            the permission/action to be granted, e.g. "read"
     * @param userID
     *            the MCRUser, whose permissions are checked           
     * @return true if the permission is granted, else false
     * @see org.mycore.common.MCRSessionMgr#getCurrentSession()
     * @see org.mycore.common.MCRSession
     */
    public boolean checkPermission(String id, String permission, String userID);

    /**
     * determines whether the current user has the permission to perform a
     * certain action.
     * 
     * All information regarding the current user is capsulated by a
     * <code>MCRSession</code> instance which can be retrieved by
     * 
     * <pre>
     * MCRSession currentSession = MCRSessionMgr.getCurrentSession();
     * </pre>
     * 
     * This method is used for checking "a priori permissions" like "create-document"
     *     where a String ID does not exist yet
     * 
     * @param permission
     *            the permission/action to be granted, e.g. "create-document"
     * @return true if the permission is granted, else false
     * @see org.mycore.common.MCRSessionMgr#getCurrentSession()
     * @see org.mycore.common.MCRSession
     */
    public boolean checkPermission(String permission);

    /**
     * determines whether a given user has the permission to perform a
     * certain action. no session data will be checked here.
     * 
     * This method is used for checking "a priori permissions" like "create-document"
     *     where a String ID does not exist yet
     * 
     * @param permission
     *            the permission/action to be granted, e.g. "create-document"
     * @param userID
     *            the MCRUser, whose permissions are checked            
     * @return true if the permission is granted, else false
     * @see org.mycore.common.MCRSessionMgr#getCurrentSession()
     * @see org.mycore.common.MCRSession
     */
    public boolean checkPermissionForUser(String permission, String userID);

    /**
     * determines whether the current user has the permission to perform a
     * certain action.
     * 
     * All information regarding the current user is capsulated by a
     * <code>MCRSession</code> instance which can be retrieved by
     * 
     * <pre>
     * MCRSession currentSession = MCRSessionMgr.getCurrentSession();
     * </pre>
     * @param rule
     *            the jdom-representation of a mycore access rule           
     * @return true if the permission is granted, else false
     * @see org.mycore.common.MCRSessionMgr#getCurrentSession()
     * @see org.mycore.common.MCRSession
     */
    public boolean checkPermission(org.jdom2.Element rule);

    /**
     * exports a access rule as JDOM element.
     * 
     * @param id
     *            the ID-String of the object
     * @param permission
     *            the access permission for the rule
     * @return the rule as jdom element, or <code>null</code> if no rule is
     *         defined
     */
    public Element getRule(String id, String permission);

    /**
     * exports a access rule for a "a priori permission"
     * as JDOM element.
     * 
     * @param permission
     *            the access permission for the rule
     * @return the rule as jdom element, or <code>null</code> if no rule is
     *         defined
     */
    public Element getRule(String permission);

    /**
     * returns the prosa description of a defined rule for a "a priori" permission like "create-document".
     * 
     * @param permission
     *            the access permission for the rule
     * @return the String of the description
     */
    public String getRuleDescription(String permission);

    /**
     * returns the prosa description of a defined rule.
     * 
     * @param id
     *            the ID-String of the object
     * @param permission
     *            the access permission for the rule
     * @return the String of the description
     */
    public String getRuleDescription(String id, String permission);

    /**
     * lists all permissions defined for the <code>id</code>.
     * 
     * The parameter <code>id</code> serves as an identifier for the concrete
     * underlying rule, e.g. a MCRObjectID.
     * 
     * @param id
     * @return a <code>List</code> of all for <code>id</code> defined
     *         permission
     */
    public Collection<String> getPermissionsForID(String id);

    /**
     * lists all a-priori permissions like "create-document".
     * 
     * @return a <code>List</code> of all defined permissions
     */
    public Collection<String> getPermissions();

    /**
     * list all object-related Access Permissions that are defined 
     * in configuration files
     * 
     * @return a List of permissiond from the configuration
     */
    public Collection<String> getAccessPermissionsFromConfiguration();

    /**
     * lists all String IDs, a permission is assigned to.
     * 
     * The parameter <code>id</code> serves as an identifier for the concrete
     * underlying rule, e.g. a MCRObjectID.
     * 
     * @return a sorted and distinct <code>List</code> of all  <code>String</code> IDs
     */
    public Collection<String> getAllControlledIDs();

    /**
     * checks wether a rule with the <code>id</code> and
     * <code>permission</code> is defined.
     * 
     * @param id
     *            the ID-String of the object
     * @param permission
     *            the access permission for the rule
     * @return false, if getRule(id, permission) would return null, else true
     */
    public boolean hasRule(String id, String permission);

    /**
     * checks wether a rule with the <code>id</code> is defined.
     * 
     * @param id
     *            the ID-String of the object
     * @return false, if getPermissionsForID(id) would return an empty list,
     *         else true
     */
    public boolean hasRule(String id);

}