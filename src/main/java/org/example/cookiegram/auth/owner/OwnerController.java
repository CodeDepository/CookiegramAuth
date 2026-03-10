package org.example.cookiegram.auth.owner;

import org.example.cookiegram.auth.security.AuthFilter;
import org.example.cookiegram.auth.security.AuthenticatedUser;
import org.example.cookiegram.auth.security.RoleChecker;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/owner")
public class OwnerController {

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(@RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user) {
        RoleChecker.requireRole(user, "OWNER");

        return ResponseEntity.ok(Map.of(
                "userRole", user.getRole(),
                "message", "Owner dashboard",
                "stats", Map.of(
                        "totalUsers", 100,
                        "activePromotions", 2,
                        "employeesOnline", 5
                )
        ));
    }
}