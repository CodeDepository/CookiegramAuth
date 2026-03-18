package org.example.cookiegram.order.service;

import org.example.cookiegram.order.repository.BlockedDateRepository;
import org.example.cookiegram.order.repository.HolidayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DateValidationServiceTest {

    @Mock
    private HolidayRepository holidays;

    @Mock
    private BlockedDateRepository blockedDates;

    @InjectMocks
    private DateValidationService dateValidation;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
    }

    // 3-day minimum

    @Test
    @DisplayName("Same day delivery is rejected")
    void rejects_today() {
        assertThatThrownBy(() -> dateValidation.validate(today))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 3 days");
    }

    @Test
    @DisplayName("Next-day delivery is rejected")
    void rejects_tomorrow() {
        assertThatThrownBy(() -> dateValidation.validate(today.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 3 days");
    }

    @Test
    @DisplayName("2-day delivery is rejected")
    void rejects_two_days_out() {
        assertThatThrownBy(() -> dateValidation.validate(today.plusDays(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 3 days");
    }

    @Test
    @DisplayName("Exactly 3 days out is accepted")
    void accepts_three_days_out() {
        assertThatCode(() -> dateValidation.validate(today.plusDays(3)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("7 days out is accepted")
    void accepts_seven_days_out() {
        assertThatCode(() -> dateValidation.validate(today.plusDays(7)))
                .doesNotThrowAnyException();
    }

    // Holiday

    @Test
    @DisplayName("Delivery on a holiday is rejected")
    void rejects_holiday() {
        LocalDate holiday = today.plusDays(5);
        when(holidays.existsByDate(holiday)).thenReturn(true);

        assertThatThrownBy(() -> dateValidation.validate(holiday))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("public holiday");
    }

    @Test
    @DisplayName("Non-holiday date is not rejected for holiday reason")
    void accepts_non_holiday() {
        LocalDate date = today.plusDays(5);
        when(holidays.existsByDate(date)).thenReturn(false);

        assertThatCode(() -> dateValidation.validate(date))
                .doesNotThrowAnyException();
    }

    // Owner-blocked date

    @Test
    @DisplayName("Delivery on an owner-blocked date is rejected")
    void rejects_blocked_date() {
        LocalDate blocked = today.plusDays(4);
        when(blockedDates.existsByDate(blocked)).thenReturn(true);

        assertThatThrownBy(() -> dateValidation.validate(blocked))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");
    }

    // isAvailable

    @Test
    @DisplayName("isAvailable returns false for < 3 days out")
    void isAvailable_rejects_too_soon() {
        assertThat(dateValidation.isAvailable(today.plusDays(2))).isFalse();
    }

    @Test
    @DisplayName("isAvailable returns false for holidays")
    void isAvailable_rejects_holiday() {
        LocalDate date = today.plusDays(5);
        when(holidays.existsByDate(date)).thenReturn(true);
        assertThat(dateValidation.isAvailable(date)).isFalse();
    }

    @Test
    @DisplayName("isAvailable returns true for a clean future date")
    void isAvailable_accepts_valid_date() {
        assertThat(dateValidation.isAvailable(today.plusDays(5))).isTrue();
    }
}
