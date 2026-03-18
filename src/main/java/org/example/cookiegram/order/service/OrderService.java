package org.example.cookiegram.order.service;

import java.io.IOException;
import org.example.cookiegram.auth.entity.User;
import org.example.cookiegram.auth.repository.UserRepository;
import org.example.cookiegram.auth.exception.UnauthorizedException;
import org.example.cookiegram.order.dto.CreateOrderRequest;
import org.example.cookiegram.order.dto.OrderResponse;
import org.example.cookiegram.order.entity.Order;
import org.example.cookiegram.order.entity.OrderStatus;
import org.example.cookiegram.order.repository.BlockedDateRepository;
import org.example.cookiegram.order.repository.HolidayRepository;
import org.example.cookiegram.order.repository.OrderRepository;
import org.example.cookiegram.payment.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    public static final BigDecimal PRICE_PER_COOKIE = new BigDecimal("5.99");

    private final OrderRepository orders;
    private final UserRepository users;
    private final DateValidationService dateValidation;
    private final HolidayRepository holidays;
    private final BlockedDateRepository blockedDates;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orders, UserRepository users,
                        DateValidationService dateValidation,
                        HolidayRepository holidays,
                        BlockedDateRepository blockedDates,
                        PaymentService paymentService) {
        this.orders = orders;
        this.users = users;
        this.dateValidation = dateValidation;
        this.holidays = holidays;
        this.blockedDates = blockedDates;
        this.paymentService = paymentService;
    }

    @Transactional
    public OrderResponse placeOrder(Long customerId, CreateOrderRequest req) {
        // 1. Validate delivery date rules
        dateValidation.validate(req.deliveryDate);

        // 2. Verify payment captured with PayPal before touching the DB
        try {
            if (!paymentService.verifySucceeded(req.paymentIntentId)) {
                throw new IllegalArgumentException("Payment has not been completed. Please complete payment before placing your order.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not verify payment: " + e.getMessage());
        }

        User customer = users.findById(customerId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        BigDecimal total = PRICE_PER_COOKIE.multiply(BigDecimal.valueOf(req.quantity));

        Order order = new Order(customer, req.quantity, req.deliveryDate,
                req.message, req.paymentIntentId, total);
        orders.save(order);

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long customerId) {
        return orders.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orders.findAllByOrderByDeliveryDateAscCreatedAtDesc()
                .stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setStatus(newStatus);
        orders.save(order);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancelMyOrder(Long orderId, Long customerId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("You can only cancel your own orders");
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException(
                    "Only confirmed orders can be cancelled. This order is " + order.getStatus().name().toLowerCase()
            );
        }

        order.setStatus(OrderStatus.CANCELLED);
        orders.save(order);
        return OrderResponse.from(order);
    }

    @Transactional
    public void deleteMyOrder(Long orderId, Long customerId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("You can only delete your own orders");
        }
        if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Only cancelled or delivered orders can be removed");
        }
        orders.delete(order);
    }

    /**
     * Returns the next {@code days} days (starting 3 days from today)
     * that are available for delivery — excluding holidays and blocked dates.
     */
    @Transactional(readOnly = true)
    public List<LocalDate> getAvailableDates(int days) {
        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = start.plusDays(days);

        // Fetch holidays and blocked dates in the range once, then filter
        var holidaySet = holidays.findAllByDateBetweenOrderByDateAsc(start, end)
                .stream()
                .map(h -> h.getDate())
                .collect(Collectors.toSet());

        var blockedSet = blockedDates.findAllByDateGreaterThanEqualOrderByDateAsc(start)
                .stream()
                .filter(b -> !b.getDate().isAfter(end))
                .map(b -> b.getDate())
                .collect(Collectors.toSet());

        return start.datesUntil(end.plusDays(1))
                .filter(d -> !holidaySet.contains(d) && !blockedSet.contains(d))
                .collect(Collectors.toList());
    }
}
