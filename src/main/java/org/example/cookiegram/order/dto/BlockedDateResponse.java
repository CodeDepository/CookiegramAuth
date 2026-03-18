package org.example.cookiegram.order.dto;

import org.example.cookiegram.order.entity.BlockedDate;

import java.time.LocalDate;

public class BlockedDateResponse {

    public Long id;
    public LocalDate date;
    public String reason;

    public static BlockedDateResponse from(BlockedDate b) {
        BlockedDateResponse r = new BlockedDateResponse();
        r.id = b.getId();
        r.date = b.getDate();
        r.reason = b.getReason();
        return r;
    }
}
