package org.example.cookiegram.order.controller;

import jakarta.validation.Valid;
import org.example.cookiegram.auth.security.AuthFilter;
import org.example.cookiegram.auth.security.AuthenticatedUser;
import org.example.cookiegram.auth.security.RoleChecker;
import org.example.cookiegram.order.dto.CreateOrderRequest;
import org.example.cookiegram.order.dto.OrderResponse;
import org.example.cookiegram.order.entity.OrderStatus;
import org.example.cookiegram.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** Customer: place an order */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user,
            @Valid @RequestBody CreateOrderRequest req) {
        RoleChecker.requireRole(user, "CUSTOMER");
        return ResponseEntity.ok(orderService.placeOrder(user.getId(), req));
    }

    /** Customer: view their own orders */
    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> myOrders(
            @RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user) {
        RoleChecker.requireRole(user, "CUSTOMER");
        return ResponseEntity.ok(orderService.getMyOrders(user.getId()));
    }

    /** Customer: fetch available delivery dates (next 60 days) */
    @GetMapping("/available-dates")
    public ResponseEntity<Map<String, Object>> availableDates(
            @RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user) {
        RoleChecker.requireRole(user, "CUSTOMER");
        List<LocalDate> dates = orderService.getAvailableDates(60);
        return ResponseEntity.ok(Map.of(
                "pricePerCookie", OrderService.PRICE_PER_COOKIE,
                "availableDates", dates
        ));
    }

    /** Employee / Owner: view all orders */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> allOrders(
            @RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user) {
        RoleChecker.requireRole(user, "EMPLOYEE", "OWNER");
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /** Customer: cancel their own order (only if CONFIRMED) */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user,
            @PathVariable Long id) {
        RoleChecker.requireRole(user, "CUSTOMER");
        return ResponseEntity.ok(orderService.cancelMyOrder(id, user.getId()));
    }

    /** Customer: permanently remove a cancelled or delivered order */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user,
            @PathVariable Long id) {
        RoleChecker.requireRole(user, "CUSTOMER");
        orderService.deleteMyOrder(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    /** Employee / Owner: update order status */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        RoleChecker.requireRole(user, "EMPLOYEE", "OWNER");
        OrderStatus status;
        try {
            status = OrderStatus.valueOf(body.get("status").toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid status value");
        }
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }
}
