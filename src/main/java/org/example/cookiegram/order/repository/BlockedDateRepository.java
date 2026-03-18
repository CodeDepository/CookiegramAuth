package org.example.cookiegram.order.repository;

import org.example.cookiegram.order.entity.BlockedDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BlockedDateRepository extends JpaRepository<BlockedDate, Long> {

    boolean existsByDate(LocalDate date);

    List<BlockedDate> findAllByDateGreaterThanEqualOrderByDateAsc(LocalDate from);
}
