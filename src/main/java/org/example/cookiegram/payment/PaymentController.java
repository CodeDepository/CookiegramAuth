package org.example.cookiegram.payment;

import org.example.cookiegram.auth.security.AuthFilter;
import org.example.cookiegram.auth.security.AuthenticatedUser;
import org.example.cookiegram.auth.security.RoleChecker;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Returns the PayPal client ID so the browser can load the PayPal JS SDK.
     * The client SECRET is never sent to the browser.
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> config(
            @RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user) {
        RoleChecker.requireRole(user, "CUSTOMER");
        return ResponseEntity.ok(Map.of(
                "clientId",   paymentService.getClientId(),
                "configured", paymentService.isConfigured()
        ));
    }

    /**
     * Creates a PayPal order and returns its ID to the frontend.
     * The frontend passes this orderID to the PayPal JS SDK which opens the
     * payment popup — card details never leave PayPal's servers.
     *
     * Only the order amount is accepted here. No card data.
     */
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user,
            @RequestBody Map<String, Object> body) {

        RoleChecker.requireRole(user, "CUSTOMER");

        Object amountRaw = body.get("amount");
        if (amountRaw == null) {
            throw new IllegalArgumentException("amount is required");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountRaw.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        try {
            String orderID = paymentService.createOrder(amount);
            return ResponseEntity.ok(Map.of("orderID", orderID));
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not create PayPal order: " + e.getMessage());
        }
    }

    /**
     * Captures an approved PayPal order (called after the user completes the popup).
     * Returns the confirmed orderID which the frontend then sends with the cookie order.
     */
    @PostMapping("/capture-order")
    public ResponseEntity<Map<String, Object>> captureOrder(
            @RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user,
            @RequestBody Map<String, Object> body) {

        RoleChecker.requireRole(user, "CUSTOMER");

        Object orderIDRaw = body.get("orderID");
        if (orderIDRaw == null || orderIDRaw.toString().isBlank()) {
            throw new IllegalArgumentException("orderID is required");
        }

        try {
            String confirmedID = paymentService.captureOrder(orderIDRaw.toString());
            return ResponseEntity.ok(Map.of(
                    "orderID", confirmedID,
                    "status",  "COMPLETED"
            ));
        } catch (IOException e) {
            throw new IllegalArgumentException("Payment capture failed: " + e.getMessage());
        }
    }
}
