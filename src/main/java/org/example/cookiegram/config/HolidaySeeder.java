package org.example.cookiegram.config;

import org.example.cookiegram.order.entity.Holiday;
import org.example.cookiegram.order.repository.HolidayRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Seeds Canadian federal statutory holidays for 2025 and 2026.
 * Runs once on startup; skips any date that's already in the DB.
 */
@Component
public class HolidaySeeder implements ApplicationRunner {

    private final HolidayRepository holidays;

    public HolidaySeeder(HolidayRepository holidays) {
        this.holidays = holidays;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Holiday> toSeed = List.of(
                // 2025
                new Holiday(LocalDate.of(2025, 1, 1),  "New Year's Day"),
                new Holiday(LocalDate.of(2025, 2, 17), "Family Day (ON)"),
                new Holiday(LocalDate.of(2025, 4, 18), "Good Friday"),
                new Holiday(LocalDate.of(2025, 5, 19), "Victoria Day"),
                new Holiday(LocalDate.of(2025, 7, 1),  "Canada Day"),
                new Holiday(LocalDate.of(2025, 8, 4),  "Civic Holiday (ON)"),
                new Holiday(LocalDate.of(2025, 9, 1),  "Labour Day"),
                new Holiday(LocalDate.of(2025, 10, 13),"Thanksgiving"),
                new Holiday(LocalDate.of(2025, 11, 11),"Remembrance Day"),
                new Holiday(LocalDate.of(2025, 12, 25),"Christmas Day"),
                new Holiday(LocalDate.of(2025, 12, 26),"Boxing Day"),
                // 2026
                new Holiday(LocalDate.of(2026, 1, 1),  "New Year's Day"),
                new Holiday(LocalDate.of(2026, 2, 16), "Family Day (ON)"),
                new Holiday(LocalDate.of(2026, 4, 3),  "Good Friday"),
                new Holiday(LocalDate.of(2026, 5, 18), "Victoria Day"),
                new Holiday(LocalDate.of(2026, 7, 1),  "Canada Day"),
                new Holiday(LocalDate.of(2026, 8, 3),  "Civic Holiday (ON)"),
                new Holiday(LocalDate.of(2026, 9, 7),  "Labour Day"),
                new Holiday(LocalDate.of(2026, 10, 12),"Thanksgiving"),
                new Holiday(LocalDate.of(2026, 11, 11),"Remembrance Day"),
                new Holiday(LocalDate.of(2026, 12, 25),"Christmas Day"),
                new Holiday(LocalDate.of(2026, 12, 26),"Boxing Day")
        );

        for (Holiday h : toSeed) {
            if (!holidays.existsByDate(h.getDate())) {
                holidays.save(h);
            }
        }
    }
}
