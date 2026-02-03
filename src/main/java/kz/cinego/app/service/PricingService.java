package kz.cinego.app.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class PricingService {

    private static final BigDecimal VIP_MULTIPLIER = new BigDecimal("1.30");
    private static final BigDecimal PEAK_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal MAX_POINTS_RATE = new BigDecimal("0.20");
    private static final BigDecimal EARN_RATE = new BigDecimal("0.01");

    public BigDecimal seatPrice(BigDecimal basePrice, String seatType, LocalDateTime startTime) {
        BigDecimal price = basePrice;

        if ("VIP".equalsIgnoreCase(seatType)) {
            price = price.multiply(VIP_MULTIPLIER);
        }

        if (isPeakHour(startTime)) {
            price = price.multiply(PEAK_MULTIPLIER);
        }

        return price.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal applyPointsDiscount(
            BigDecimal total,
            int availablePoints,
            int pointsToUse
    ) {
        if (pointsToUse <= 0) {
            return total;
        }

        int usablePoints = Math.min(pointsToUse, availablePoints);

        BigDecimal maxDiscount = total.multiply(MAX_POINTS_RATE);
        BigDecimal requested = BigDecimal.valueOf(usablePoints);

        BigDecimal discount = requested.min(maxDiscount);
        BigDecimal result = total.subtract(discount);

        return result.max(BigDecimal.ZERO);
    }

    public int earnedPoints(BigDecimal paidTotal) {
        return paidTotal.multiply(EARN_RATE).intValue();
    }

    private boolean isPeakHour(LocalDateTime time) {
        int hour = time.getHour();
        return hour >= 18 && hour < 22;
    }
}
