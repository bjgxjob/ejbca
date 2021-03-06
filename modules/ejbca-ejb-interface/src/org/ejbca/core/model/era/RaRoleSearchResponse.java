/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.core.model.era;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cesecore.roles.Role;

/**
 * Response of role search from RA UI.
 * 
 * @version $Id$
 */
public class RaRoleSearchResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private List<Role> roles = new ArrayList<>();
    private boolean mightHaveMoreResults = false;

    public List<Role> getRoles() { return roles; }
    public void setRoles(List<Role> roles) { this.roles = roles; }

    public boolean isMightHaveMoreResults() { return mightHaveMoreResults; }
    public void setMightHaveMoreResults(boolean mightHaveMoreResults) { this.mightHaveMoreResults = mightHaveMoreResults; }
    
    public void merge(final RaRoleSearchResponse other) {
        final Map<Integer,Role> roleMap = new HashMap<>();
        for (final Role role : roles) {
            roleMap.put(role.getRoleId(), role);
        }
        for (final Role role : other.roles) {
            roleMap.put(role.getRoleId(), role);
        }
        this.roles.clear();
        this.roles.addAll(roleMap.values());
        if (other.isMightHaveMoreResults()) {
            setMightHaveMoreResults(true);
        }
    }
}
