package org.example.cookiegram.order.repository;

import org.example.cookiegram.order.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    boolean existsByDate(LocalDate date);

    List<Holiday> findAllByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);
}
