package org.example.cookiegram.order.dto;

import org.example.cookiegram.order.entity.Order;
import org.example.cookiegram.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class OrderResponse {

    public Long id;
    public String customerUsername;
    public int quantity;
    public LocalDate deliveryDate;
    public String message;
    public OrderStatus status;
    public BigDecimal totalAmount;
    public Instant createdAt;

    public static OrderResponse from(Order o) {
        OrderResponse r = new OrderResponse();
        r.id = o.getId();
        r.customerUsername = o.getCustomer().getUsername();
        r.quantity = o.getQuantity();
        r.deliveryDate = o.getDeliveryDate();
        r.message = o.getMessage();
        r.status = o.getStatus();
        r.totalAmount = o.getTotalAmount();
        r.createdAt = o.getCreatedAt();
        return r;
    }
}
