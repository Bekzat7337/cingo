package kz.cinego.app.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Screening(long id, long movieId, long hallId, LocalDateTime startTime, BigDecimal basePrice) {}
