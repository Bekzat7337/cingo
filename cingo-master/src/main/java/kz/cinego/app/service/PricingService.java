package kz.cinego.app.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class PricingService {
    private static final BigDecimal VIP_MULT = new BigDecimal("1.30");
    private static final BigDecimal PEAK_MULT = new BigDecimal("1.20");

    public BigDecimal seatPrice(BigDecimal base, String seatType, LocalDateTime startTime) {
        BigDecimal price = base;

        if ("VIP".equalsIgnoreCase(seatType)) price = price.multiply(VIP_MULT);
        if (isPeak(startTime)) price = price.multiply(PEAK_MULT);

        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isPeak(LocalDateTime t) {
        int h = t.getHour();
        return h >= 18 && h < 22;
    }

    public BigDecimal applyPointsDiscount(BigDecimal total, int availablePoints, int pointsToUse) {
        if (pointsToUse <= 0) return total;

        int safeUse = Math.min(pointsToUse, availablePoints);
        BigDecimal maxDiscount = total.multiply(new BigDecimal("0.20"));
        BigDecimal req = new BigDecimal(safeUse);

        BigDecimal discount = req.min(maxDiscount);
        BigDecimal result = total.subtract(discount);
        return result.max(BigDecimal.ZERO);
    }

    public int earnedPoints(BigDecimal paidTotal) {
        return paidTotal.multiply(new BigDecimal("0.01")).intValue();
    }
}
