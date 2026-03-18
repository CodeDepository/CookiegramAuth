package org.example.cookiegram.order.entity;

import jakarta.persistence.*;
import org.example.cookiegram.auth.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private LocalDate deliveryDate;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.CONFIRMED;

    /** Payment intent reference from mock payment processor. No card data stored here. */
    @Column(nullable = false, length = 100)
    private String paymentIntentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Order() {}

    public Order(User customer, int quantity, LocalDate deliveryDate,
                 String message, String paymentIntentId, BigDecimal totalAmount) {
        this.customer = customer;
        this.quantity = quantity;
        this.deliveryDate = deliveryDate;
        this.message = message;
        this.paymentIntentId = paymentIntentId;
        this.totalAmount = totalAmount;
    }

    public Long getId() { return id; }
    public User getCustomer() { return customer; }
    public int getQuantity() { return quantity; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public String getMessage() { return message; }
    public OrderStatus getStatus() { return status; }
    public String getPaymentIntentId() { return paymentIntentId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(OrderStatus status) { this.status = status; }
}
