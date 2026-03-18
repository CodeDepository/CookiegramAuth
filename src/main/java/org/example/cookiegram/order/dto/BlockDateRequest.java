package org.example.cookiegram.order.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class BlockDateRequest {

    @NotNull(message = "Date is required")
    public LocalDate date;

    public String reason;
}
