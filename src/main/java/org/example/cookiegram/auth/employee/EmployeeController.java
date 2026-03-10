package org.example.cookiegram.auth.employee;

import org.example.cookiegram.auth.security.AuthFilter;
import org.example.cookiegram.auth.security.AuthenticatedUser;
import org.example.cookiegram.auth.security.RoleChecker;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(@RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user) {
        RoleChecker.requireRole(user, "EMPLOYEE", "OWNER");

        return ResponseEntity.ok(Map.of(
                "userRole", user.getRole(),
                "message", "Employee dashboard",
                "tasks", List.of("Check orders", "Update promotions", "Respond to customer issues")
        ));
    }
}