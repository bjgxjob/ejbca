/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.cesecore.roles.member;

import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;

/**
 * Session bean for managing role members, taking authorization of the authentication token into account.
 * 
 * @version $Id$
 *
 */
public interface RoleMemberSession {

    /**
     * Returns the Role Member with the given ID, or null if it does not exist.
     * @throws AuthorizationDeniedException if not authorized to the Role or the CA in the RoleMember object.
     */
    RoleMember getRoleMember(AuthenticationToken authenticationToken, int roleMemberId) throws AuthorizationDeniedException;

    /**
     * Adds or updates a Role Member (depending on the value of the ID).
     * @return The ID of the role member after persisting it.
     * @throws AuthorizationDeniedException If access was denied to editing this role member or the referenced CA or Role.
     */
    int createOrEdit(AuthenticationToken authenticationToken, RoleMember roleMember) throws AuthorizationDeniedException;

    /**
     * Deletes the role member with the specified ID.
     * @return true if successfully deleted, false if it did not exist.
     */
    boolean remove(final AuthenticationToken authenticationToken, final int roleMemberId) throws AuthorizationDeniedException;
}
