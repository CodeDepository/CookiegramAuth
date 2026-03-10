package org.example.cookiegram.auth.security;

import org.example.cookiegram.auth.exception.UnauthorizedException;

public class RoleChecker {

    public static void requireRole(AuthenticatedUser user, String... allowedRoles) {
        for (String role : allowedRoles) {
            if (role.equalsIgnoreCase(user.getRole())) {
                return;
            }
        }
        throw new UnauthorizedException("You do not have permission to access this resource");
    }
}