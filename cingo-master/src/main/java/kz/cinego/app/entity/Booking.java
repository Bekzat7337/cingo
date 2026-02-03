package kz.cinego.app.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Booking(
        long id,
        long userId,
        long screeningId,
        String status,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt,
        BigDecimal refundAmount
) {}
