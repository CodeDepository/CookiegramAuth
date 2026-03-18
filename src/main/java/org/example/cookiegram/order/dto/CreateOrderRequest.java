package org.example.cookiegram.order.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CreateOrderRequest {

    @NotNull(message = "Delivery date is required")
    public LocalDate deliveryDate;

    @Min(value = 1, message = "Minimum 1 cookie")
    @Max(value = 100, message = "Maximum 100 cookies per order")
    public int quantity = 1;

    public String message;

    @NotNull(message = "Payment intent ID is required")
    public String paymentIntentId;
}
