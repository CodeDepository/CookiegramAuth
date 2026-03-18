package org.example.cookiegram.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CreateOrderRequest {

    @NotNull(message = "Delivery date is required")
    public LocalDate deliveryDate;

    @Min(value = 1, message = "Minimum 1 cookie")
    @Max(value = 100, message = "Maximum 100 cookies per order")
    public int quantity = 1;

    public String message;

    @NotBlank(message = "Street address is required")
    public String streetAddress;

    @NotBlank(message = "City is required")
    public String city;

    @NotBlank(message = "Province is required")
    public String province;

    @NotBlank(message = "Postal code is required")
    public String postalCode;

    @NotNull(message = "Payment intent ID is required")
    public String paymentIntentId;
}
