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

    @Column(length = 200)
    private String streetAddress;

    @Column(length = 100)
    private String city;

    @Column(length = 50)
    private String province;

    @Column(length = 10)
    private String postalCode;

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
                 String message, String streetAddress, String city, String province,
                 String postalCode, String paymentIntentId, BigDecimal totalAmount) {
        this.customer = customer;
        this.quantity = quantity;
        this.deliveryDate = deliveryDate;
        this.message = message;
        this.streetAddress = streetAddress;
        this.city = city;
        this.province = province;
        this.postalCode = postalCode;
        this.paymentIntentId = paymentIntentId;
        this.totalAmount = totalAmount;
    }

    public Long getId() { return id; }
    public User getCustomer() { return customer; }
    public int getQuantity() { return quantity; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public String getMessage() { return message; }
    public String getStreetAddress() { return streetAddress; }
    public String getCity() { return city; }
    public String getProvince() { return province; }
    public String getPostalCode() { return postalCode; }
    public OrderStatus getStatus() { return status; }
    public String getPaymentIntentId() { return paymentIntentId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(OrderStatus status) { this.status = status; }
}
