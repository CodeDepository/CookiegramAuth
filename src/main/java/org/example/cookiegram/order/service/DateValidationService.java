package org.example.cookiegram.order.service;

import org.example.cookiegram.order.repository.BlockedDateRepository;
import org.example.cookiegram.order.repository.HolidayRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DateValidationService {

    private final HolidayRepository holidays;
    private final BlockedDateRepository blockedDates;

    public DateValidationService(HolidayRepository holidays, BlockedDateRepository blockedDates) {
        this.holidays = holidays;
        this.blockedDates = blockedDates;
    }

    /**
     * Throws IllegalArgumentException if the delivery date is not valid.
     * Rules:
     *  1. Must be at least 3 days from today (not counting today).
     *  2. Must not fall on a public holiday.
     *  3. Must not be manually blocked by the owner.
     */
    public void validate(LocalDate deliveryDate) {
        LocalDate today = LocalDate.now();
        LocalDate earliest = today.plusDays(3);

        if (!deliveryDate.isAfter(earliest.minusDays(1))) {
            throw new IllegalArgumentException(
                    "Delivery date must be at least 3 days from today (earliest: " + earliest + ")"
            );
        }

        if (holidays.existsByDate(deliveryDate)) {
            throw new IllegalArgumentException(
                    "Delivery date " + deliveryDate + " falls on a public holiday. Please choose another date."
            );
        }

        if (blockedDates.existsByDate(deliveryDate)) {
            throw new IllegalArgumentException(
                    "Delivery date " + deliveryDate + " is not available. Please choose another date."
            );
        }
    }

    /** Returns true if the date is available for delivery. */
    public boolean isAvailable(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (!date.isAfter(today.plusDays(2))) return false;
        if (holidays.existsByDate(date)) return false;
        if (blockedDates.existsByDate(date)) return false;
        return true;
    }
}
