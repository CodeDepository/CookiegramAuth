package org.example.cookiegram.order.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "blocked_dates")
public class BlockedDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(length = 200)
    private String reason;

    /** ID of the owner who blocked this date */
    @Column(nullable = false)
    private Long blockedBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public BlockedDate() {}

    public BlockedDate(LocalDate date, String reason, Long blockedBy) {
        this.date = date;
        this.reason = reason;
        this.blockedBy = blockedBy;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getReason() { return reason; }
    public Long getBlockedBy() { return blockedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
