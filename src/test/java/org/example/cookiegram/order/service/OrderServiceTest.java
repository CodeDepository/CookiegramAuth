package org.example.cookiegram.order.service;

import java.io.IOException;
import org.example.cookiegram.auth.entity.User;
import org.example.cookiegram.auth.entity.UserRole;
import org.example.cookiegram.auth.repository.UserRepository;
import org.example.cookiegram.order.dto.CreateOrderRequest;
import org.example.cookiegram.order.dto.OrderResponse;
import org.example.cookiegram.order.entity.Order;
import org.example.cookiegram.order.entity.OrderStatus;
import org.example.cookiegram.order.repository.BlockedDateRepository;
import org.example.cookiegram.order.repository.HolidayRepository;
import org.example.cookiegram.order.repository.OrderRepository;
import org.example.cookiegram.payment.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orders;
    @Mock private UserRepository users;
    @Mock private DateValidationService dateValidation;
    @Mock private HolidayRepository holidays;
    @Mock private BlockedDateRepository blockedDates;
    @Mock private PaymentService paymentService;

    @InjectMocks
    private OrderService orderService;

    private User mockCustomer;

    @BeforeEach
    void setUp() {
        mockCustomer = new User("alice", "alice@example.com", "hashed", UserRole.CUSTOMER);
    }

    /* ─────────────────────────────────────────────────
       placeOrder — success path
       ───────────────────────────────────────────────── */

    @Test
    @DisplayName("Places an order when date is valid and payment succeeded")
    void places_order_successfully() throws IOException {
        // Arrange
        CreateOrderRequest req = new CreateOrderRequest();
        req.deliveryDate    = LocalDate.now().plusDays(5);
        req.quantity        = 2;
        req.message         = "Happy Birthday!";
        req.paymentIntentId = "pi_test_123";

        doNothing().when(dateValidation).validate(req.deliveryDate);
        when(paymentService.verifySucceeded("pi_test_123")).thenReturn(true);
        when(users.findById(1L)).thenReturn(Optional.of(mockCustomer));
        when(orders.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        OrderResponse response = orderService.placeOrder(1L, req);

        // Assert
        assertThat(response.quantity).isEqualTo(2);
        assertThat(response.deliveryDate).isEqualTo(req.deliveryDate);
        assertThat(response.message).isEqualTo("Happy Birthday!");
        assertThat(response.status).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.totalAmount)
                .isEqualByComparingTo(new BigDecimal("11.98")); // 2 × $5.99

        verify(orders).save(any(Order.class));
    }

    @Test
    @DisplayName("Total amount is correctly calculated for 1 cookie")
    void calculates_single_cookie_total() throws IOException {
        CreateOrderRequest req = new CreateOrderRequest();
        req.deliveryDate    = LocalDate.now().plusDays(5);
        req.quantity        = 1;
        req.paymentIntentId = "pi_test_1";

        doNothing().when(dateValidation).validate(any());
        when(paymentService.verifySucceeded(any())).thenReturn(true);
        when(users.findById(anyLong())).thenReturn(Optional.of(mockCustomer));
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.placeOrder(1L, req);

        assertThat(response.totalAmount).isEqualByComparingTo(new BigDecimal("5.99"));
    }

    @Test
    @DisplayName("Total amount is correctly calculated for 10 cookies")
    void calculates_ten_cookie_total() throws IOException {
        CreateOrderRequest req = new CreateOrderRequest();
        req.deliveryDate    = LocalDate.now().plusDays(5);
        req.quantity        = 10;
        req.paymentIntentId = "pi_test_10";

        doNothing().when(dateValidation).validate(any());
        when(paymentService.verifySucceeded(any())).thenReturn(true);
        when(users.findById(anyLong())).thenReturn(Optional.of(mockCustomer));
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.placeOrder(1L, req);

        assertThat(response.totalAmount).isEqualByComparingTo(new BigDecimal("59.90"));
    }

    /* ─────────────────────────────────────────────────
       placeOrder — payment failure paths
       ───────────────────────────────────────────────── */

    @Test
    @DisplayName("Order is rejected when payment has not succeeded")
    void rejects_order_when_payment_not_succeeded() throws IOException {
        CreateOrderRequest req = new CreateOrderRequest();
        req.deliveryDate    = LocalDate.now().plusDays(5);
        req.quantity        = 1;
        req.paymentIntentId = "pi_pending_456";

        doNothing().when(dateValidation).validate(any());
        when(paymentService.verifySucceeded("pi_pending_456")).thenReturn(false);

        assertThatThrownBy(() -> orderService.placeOrder(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment has not been completed");

        verify(orders, never()).save(any()); // DB must NOT be touched
    }

    @Test
    @DisplayName("Order is rejected when PayPal throws an IOException")
    void rejects_order_on_paypal_exception() throws IOException {
        CreateOrderRequest req = new CreateOrderRequest();
        req.deliveryDate    = LocalDate.now().plusDays(5);
        req.quantity        = 1;
        req.paymentIntentId = "ORDER_BAD";

        doNothing().when(dateValidation).validate(any());
        when(paymentService.verifySucceeded("ORDER_BAD"))
                .thenThrow(new IOException("PayPal API error"));

        assertThatThrownBy(() -> orderService.placeOrder(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Could not verify payment");

        verify(orders, never()).save(any());
    }

    /* ─────────────────────────────────────────────────
       placeOrder — date validation failure
       ───────────────────────────────────────────────── */

    @Test
    @DisplayName("Order is rejected when date validation fails")
    void rejects_order_when_date_invalid() throws IOException {
        CreateOrderRequest req = new CreateOrderRequest();
        req.deliveryDate    = LocalDate.now().plusDays(1); // too soon
        req.quantity        = 1;
        req.paymentIntentId = "pi_test_789";

        doThrow(new IllegalArgumentException("at least 3 days"))
                .when(dateValidation).validate(req.deliveryDate);

        assertThatThrownBy(() -> orderService.placeOrder(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 3 days");

        // Payment should never be verified — date check comes first
        verify(paymentService, never()).verifySucceeded(any());
        verify(orders, never()).save(any());
    }

    /* ─────────────────────────────────────────────────
       status update
       ───────────────────────────────────────────────── */

    @Test
    @DisplayName("updateStatus changes the order status and saves")
    void updates_order_status() {
        Order order = new Order(mockCustomer, 1, LocalDate.now().plusDays(5),
                null, "pi_test", new BigDecimal("5.99"));

        when(orders.findById(42L)).thenReturn(Optional.of(order));
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.updateStatus(42L, OrderStatus.PREPARING);

        assertThat(response.status).isEqualTo(OrderStatus.PREPARING);
        verify(orders).save(order);
    }

    @Test
    @DisplayName("updateStatus throws when order not found")
    void throws_when_order_not_found() {
        when(orders.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateStatus(99L, OrderStatus.DELIVERED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    /* ─────────────────────────────────────────────────
       getMyOrders
       ───────────────────────────────────────────────── */

    @Test
    @DisplayName("getMyOrders returns empty list when customer has no orders")
    void returns_empty_list_for_no_orders() {
        when(orders.findByCustomerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        List<OrderResponse> result = orderService.getMyOrders(1L);

        assertThat(result).isEmpty();
    }
}
