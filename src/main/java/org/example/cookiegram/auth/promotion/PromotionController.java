package org.example.cookiegram.auth.promotion;

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
@RequestMapping("/api/promotions")
public class PromotionController {

    @GetMapping("/my")
    public ResponseEntity<?> myPromotions(@RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user) {
        RoleChecker.requireRole(user, "CUSTOMER");

        return ResponseEntity.ok(Map.of(
                "userRole", user.getRole(),
                "promotions", List.of(
                        Map.of("title", "10% Off Cookies", "description", "Available this week only"),
                        Map.of("title", "Free Delivery", "description", "Orders over $20")
                )
        ));
    }
}