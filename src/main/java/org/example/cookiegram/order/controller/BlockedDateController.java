package org.example.cookiegram.order.controller;

import jakarta.validation.Valid;
import org.example.cookiegram.auth.security.AuthFilter;
import org.example.cookiegram.auth.security.AuthenticatedUser;
import org.example.cookiegram.auth.security.RoleChecker;
import org.example.cookiegram.order.dto.BlockDateRequest;
import org.example.cookiegram.order.dto.BlockedDateResponse;
import org.example.cookiegram.order.entity.BlockedDate;
import org.example.cookiegram.order.repository.BlockedDateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/owner/blocked-dates")
public class BlockedDateController {

    private final BlockedDateRepository blockedDates;

    public BlockedDateController(BlockedDateRepository blockedDates) {
        this.blockedDates = blockedDates;
    }

    @GetMapping
    public ResponseEntity<List<BlockedDateResponse>> list(
            @RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user) {
        RoleChecker.requireRole(user, "OWNER");
        List<BlockedDateResponse> result = blockedDates
                .findAllByDateGreaterThanEqualOrderByDateAsc(LocalDate.now())
                .stream()
                .map(BlockedDateResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<BlockedDateResponse> block(
            @RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user,
            @Valid @RequestBody BlockDateRequest req) {
        RoleChecker.requireRole(user, "OWNER");

        if (blockedDates.existsByDate(req.date)) {
            throw new IllegalArgumentException("Date " + req.date + " is already blocked");
        }

        BlockedDate saved = blockedDates.save(new BlockedDate(req.date, req.reason, user.getId()));
        return ResponseEntity.ok(BlockedDateResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> unblock(
            @RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user,
            @PathVariable Long id) {
        RoleChecker.requireRole(user, "OWNER");

        if (!blockedDates.existsById(id)) {
            throw new IllegalArgumentException("Blocked date not found");
        }
        blockedDates.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Date unblocked"));
    }
}
