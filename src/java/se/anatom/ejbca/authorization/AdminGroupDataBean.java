/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package se.anatom.ejbca.authorization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.RemoveException;

import org.apache.log4j.Logger;

import se.anatom.ejbca.BaseEntityBean;
import se.anatom.ejbca.util.ServiceLocator;


/** Entity bean should not be used directly, use though Session beans.
 *
 * Entity Bean representing authorization admingroup.
 * Information stored:
 * <pre>
 * admingroupname
 * caid
 *
 * AccessRules
 * Admin entities
 * </pre>
 *
 * @version $Id: AdminGroupDataBean.java,v 1.12 2005-02-23 15:26:56 anatom Exp $
 *
 * @ejb.bean
 *   description="This enterprise bean entity represents an authorization usergroup"
 *   display-name="AdminGroupDataEB"
 *   name="AdminGroupData"
 *   view-type="local"
 *   type="CMP"
 *   reentrant="false"
 *   cmp-version="2.x"
 *   transaction-type="Container"
 *   schema="AdminGroupDataBean"
 *   primkey-field="PK"
 *
 * @ejb.permission role-name="InternalUser"
 *
 * @ejb.home
 *   generate="local"
 *   local-extends="javax.ejb.EJBLocalHome"
 *   local-class="se.anatom.ejbca.authorization.AdminGroupDataLocalHome"
 *
 * @ejb.interface
 *   generate="local"
 *   local-extends="javax.ejb.EJBLocalObject"
 *   local-class="se.anatom.ejbca.authorization.AdminGroupDataLocal"
 *
 * @ejb.finder
 *   description="findByGroupNameAndCAId"
 *   signature="se.anatom.ejbca.authorization.AdminGroupDataLocal findByGroupNameAndCAId(java.lang.String name,  int id)"
 *   query="SELECT DISTINCT OBJECT(a) from AdminGroupDataBean a WHERE a.adminGroupName=?1 AND a.CAId=?2"
 *
 * @ejb.finder
 *   description="findAll"
 *   signature="java.util.Collection findAll()"
 *   query="SELECT DISTINCT OBJECT(a) from AdminGroupDataBean a"
 *
 * @ejb.ejb-external-ref
 *   description=""
 *   view-type="local"
 *   ejb-name="AdminEntityDataLocal"
 *   type="Entity"
 *   home="se.anatom.ejbca.authorization.AdminEntityDataLocalHome"
 *   business="se.anatom.ejbca.authorization.AdminEntityDataLocal"
 *   link="AdminEntityData"
 *
 * @ejb.ejb-external-ref
 *   description=""
 *   view-type="local"
 *   ejb-name="AccessRulesDataLocal"
 *   type="Entity"
 *   home="se.anatom.ejbca.authorization.AccessRulesDataLocalHome"
 *   business="se.anatom.ejbca.authorization.AccessRulesDataLocal"
 *   link="AccessRulesData"
 *
 */
public abstract class AdminGroupDataBean extends BaseEntityBean {

    private static final Logger log = Logger.getLogger(AdminGroupDataBean.class);

    /**
     * @ejb.persistence
     * @ejb.pk-field
     */
    public abstract Integer getPK();

    /**
     * @ejb.persistence
     */
    public abstract void setPK(Integer pk);

    /**
     * @ejb.persistence
     * @ejb.interface-method view-type="local"
     */
    public abstract String getAdminGroupName();

    /**
     * @ejb.persistence
     * @ejb.interface-method view-type="local"
     */
    public abstract void setAdminGroupName(String admingroupname);

    /**
     * @ejb.persistence
     * @ejb.interface-method view-type="local"
     */
    public abstract int getCAId();

    /**
     * @ejb.persistence
     * @ejb.interface-method view-type="local"
     */
    public abstract void setCAId(int caid);

    /**
     * @ejb.relation name="AdminGroupDataToAdminEntities" role-name="AdminGroupData"
     * target-role-name="AdminEntityData" target-ejb="AdminEntityData"
     * 
     * @jboss.target-relation
     * related-pk-field="PK"
     * fk-column="AdminGroup_AdminEntities"  
     * 
     * @weblogic.target-column-map
     * key-column="PK"
     * foreign-key-column="AdminGroup_AdminEntities"
     * 
     * @sunone.relation
     * column="PK"
     * target="AdminGroup_AdminEntities"
     */
    public abstract Collection getAdminEntities();
    public abstract void setAdminEntities(Collection adminentities);

    /**
     * @ejb.relation
     * name="AdminGroupDataToAccessRules" role-name="AdminGroupData"
     * target-role-name="AccessRulesData" target-ejb="AccessRulesData"
     * 
     * @jboss.target-relation
     * related-pk-field="PK"
     * fk-column="AdminGroup_AccessRules"
     *      
     * @weblogic.target-column-map
     * key-column="PK"
     * foreign-key-column="AdminGroup_AccessRules"
     * 
     * @sunone.relation
     * column="PK"
     * target="AdminGroup_AccessRules"
     */
    public abstract Collection getAccessRules();
    public abstract void setAccessRules(Collection accessrules);

    /**
     * Adds a Collection of AccessRule to the database. Changing their values if they already exists
     * @ejb.interface-method view-type="local"
     */
    public void addAccessRules(Collection accessrules) {
        Iterator iter = accessrules.iterator();
        while (iter.hasNext()) {
            AccessRule accessrule = (AccessRule) iter.next();
            try {
                AccessRulesDataLocal data = createAccessRule(accessrule);

                Iterator i = getAccessRules().iterator();
                while (i.hasNext()) {
                    AccessRulesDataLocal ar = (AccessRulesDataLocal) i.next();
                    if (ar.getAccessRuleObject().getAccessRule().equals(accessrule.getAccessRule())) {
                        getAccessRules().remove(ar);
                        try {
                            ar.remove();
                        } catch (RemoveException e) {
                            throw new EJBException(e.getMessage());
                        }
                        break;
                    }
                }

                getAccessRules().add(data);
            } catch (Exception e) {
            }
        }
    } // addAccessRules

    /**
     * Removes a Collection of (String) accessrules from the database.
     * @ejb.interface-method view-type="local"
     */
    public void removeAccessRules(Collection accessrules) {
        Iterator iter = accessrules.iterator();
        while (iter.hasNext()) {
            String accessrule = (String) iter.next();

            Iterator i = getAccessRules().iterator();
            while (i.hasNext()) {
                AccessRulesDataLocal ar = (AccessRulesDataLocal) i.next();
                if (ar.getAccessRuleObject().getAccessRule().equals(accessrule)) {
                    getAccessRules().remove(ar);
                    try {
                        ar.remove();
                    } catch (RemoveException e) {
                        throw new EJBException(e.getMessage());
                    }
                    break;
                }
            }
        }
    } // removeAccessRules

    /**
     * Returns the number of access rules in admingroup
     *
     * @return the number of accessrules in the database
     * @ejb.interface-method view-type="local"
     */
    public int getNumberOfAccessRules() {
        return getAccessRules().size();
    } // getNumberOfAccessRules

    /**
     * Returns all the accessrules as a Collection of AccessRules
     * @ejb.interface-method view-type="local"
     */
    public Collection getAccessRuleObjects() {
        final Collection rules = getAccessRules();
        ArrayList objects = new ArrayList(rules.size());
        Iterator i = rules.iterator();
        while (i.hasNext()) {
            AccessRulesDataLocal ar = (AccessRulesDataLocal) i.next();
            objects.add(ar.getAccessRuleObject());
        }
        return objects;
    }

    /**
     * Adds a Collection of AdminEntity to the database. Changing their values if they already exists
     * @ejb.interface-method view-type="local"
     */

    public void addAdminEntities(Collection adminentities) {
        Iterator iter = adminentities.iterator();
        while (iter.hasNext()) {
            AdminEntity adminentity = (AdminEntity) iter.next();
            try {
                AdminEntityDataLocal data = createAdminEntity(adminentity);
                AdminEntityPK datapk = createAdminEntityPK(getAdminGroupName(), getCAId(), adminentity.getMatchWith(), adminentity.getMatchType(), adminentity.getMatchValue());

                Iterator i = getAdminEntities().iterator();
                while (i.hasNext()) {
                    AdminEntityDataLocal ue = (AdminEntityDataLocal) i.next();
                    // TODO use ue.getPrimaryKey() ?
                    AdminEntityPK uepk = createAdminEntityPK(getAdminGroupName(), getCAId(), ue.getMatchWith()
                            , ue.getMatchType(), ue.getMatchValue());
                    if (uepk.equals(datapk)) {
                        getAdminEntities().remove(ue);
                        try {
                            ue.remove();
                        } catch (RemoveException e) {
                            throw new EJBException(e.getMessage());
                        }
                        break;
                    }
                }
                getAdminEntities().add(data);
            } catch (Exception e) {
            }
        }
    } // addAdminEntities


    /**
     * Removes a Collection if AdminEntity from the database.
     * @ejb.interface-method view-type="local"
     */
    public void removeAdminEntities(Collection adminentities) {
        Iterator iter = adminentities.iterator();

        while (iter.hasNext()) {
            AdminEntity adminentity = (AdminEntity) iter.next();
            AdminEntityPK datapk = createAdminEntityPK(getAdminGroupName(), getCAId(), adminentity.getMatchWith(), adminentity.getMatchType(), adminentity.getMatchValue());

            Iterator i = getAdminEntities().iterator();
            while (i.hasNext()) {
                AdminEntityDataLocal ue = (AdminEntityDataLocal) i.next();
                // TODO use ue.getPrimaryKey() ?
                AdminEntityPK uepk = createAdminEntityPK(getAdminGroupName(), getCAId(), ue.getMatchWith(), ue.getMatchType(), ue.getMatchValue());
                if (uepk.equals(datapk)) {
                    getAdminEntities().remove(ue);
                    try {
                        ue.remove();
                    } catch (RemoveException e) {
                        throw new EJBException(e);
                    }
                    break;
                }
            }
        }
    } // removeAdminEntities

    // this method is to avoid matching arguments errors while generating the class
    private AdminEntityPK createAdminEntityPK(String name, int id, int with, int type, String value){
        AdminEntityPK pk = new AdminEntityPK();
        pk.setAdminGroupName(name);
        pk.setCaId(id);
        pk.setMatchWith(with);
        pk.setMatchType(type);
        pk.setMatchValue(value);
        return pk;
    }


    /**
     * Returns the number of user entities in admingroup
     *
     * @return the number of user entities in the database
     * @ejb.interface-method view-type="local"
     */
    public int getNumberOfAdminEntities() {
        return getAdminEntities().size();
    } // getNumberOfAdminEntities

    /**
     * Returns all the adminentities as Collection of AdminEntity.
     * @ejb.interface-method view-type="local"
     */
    public Collection getAdminEntityObjects() {
        ArrayList returnval = new ArrayList();
        Iterator i = getAdminEntities().iterator();
        while (i.hasNext()) {
            AdminEntityDataLocal ae = (AdminEntityDataLocal) i.next();
            returnval.add(ae.getAdminEntity(getCAId()));
        }
        return returnval;
    } // getAdminEntityObjects

    /**
     * Returns the data in admingroup representation.
     * @ejb.interface-method view-type="local"
     */
    public AdminGroup getAdminGroup() {
        ArrayList accessrules = new ArrayList();
        ArrayList adminentities = new ArrayList();

        Iterator i = null;
        i = getAdminEntities().iterator();
        while (i.hasNext()) {
            AdminEntityDataLocal ae = (AdminEntityDataLocal) i.next();
            adminentities.add(ae.getAdminEntity(getCAId()));
        }

        i = getAccessRules().iterator();
        while (i.hasNext()) {
            AccessRulesDataLocal ar = (AccessRulesDataLocal) i.next();
            accessrules.add(ar.getAccessRuleObject());
        }

        return new AdminGroup(getPK().intValue(), getAdminGroupName(), getCAId(), accessrules, adminentities);
    } // getAdminGroup

    /**
     * Returns an AdminGroup object only containing name and caid and no access data.
     * @ejb.interface-method view-type="local"
     */
    public AdminGroup getAdminGroupNames() {
        return new AdminGroup(getPK().intValue(), getAdminGroupName(), getCAId(), null, null);
    } // getAdminGroupNames
    //
    // Fields required by Container
    //

    /**
     * Entity Bean holding data of raadmin profilegroups.
     * @param admingroupname
     *
     * @ejb.create-method view-type="local"
     */
    public Integer ejbCreate(Integer pk, String admingroupname, int caid) throws CreateException {
        setPK(pk);
        setAdminGroupName(admingroupname);
        setCAId(caid);
        log.debug("Created admingroup : " + admingroupname);

        return pk;
    }

    public void ejbPostCreate(Integer pk, String admingroupname, int caid) {

        // Do nothing. Required.

    }

    // Private Methods.
    private AdminEntityDataLocal createAdminEntity(AdminEntity adminentity) throws CreateException {
        AdminEntityDataLocalHome home = (AdminEntityDataLocalHome) ServiceLocator.getInstance().getLocalHome(AdminEntityDataLocalHome.COMP_NAME);
        AdminEntityDataLocal returnval = home.create(getAdminGroupName(), getCAId(), adminentity.getMatchWith(),
                adminentity.getMatchType(), adminentity.getMatchValue());
        return returnval;
    }

    private AccessRulesDataLocal createAccessRule(AccessRule accessrule) throws CreateException {
        AccessRulesDataLocalHome home = (AccessRulesDataLocalHome) ServiceLocator.getInstance().getLocalHome(AccessRulesDataLocalHome.COMP_NAME);
        AccessRulesDataLocal returnval = home.create(getAdminGroupName(), getCAId(), accessrule.getAccessRule(),
                accessrule.getRule(), accessrule.isRecursive());
        return returnval;
    }
}
