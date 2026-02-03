package kz.cinego.app.service;

import kz.cinego.app.db.Database;
import kz.cinego.app.entity.Booking;
import kz.cinego.app.entity.Screening;
import kz.cinego.app.entity.Seat;
import kz.cinego.app.entity.User;
import kz.cinego.app.repository.BookingRepository;
import kz.cinego.app.repository.ScreeningRepository;
import kz.cinego.app.repository.SeatRepository;
import kz.cinego.app.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BookingService {

    private final ScreeningRepository screeningRepo;
    private final SeatRepository seatRepo;
    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;
    private final PricingService pricing;

    public BookingService(ScreeningRepository screeningRepo,
                          SeatRepository seatRepo,
                          BookingRepository bookingRepo,
                          UserRepository userRepo,
                          PricingService pricing) {
        this.screeningRepo = screeningRepo;
        this.seatRepo = seatRepo;
        this.bookingRepo = bookingRepo;
        this.userRepo = userRepo;
        this.pricing = pricing;
    }

    public record CreateBookingResult(long bookingId, BigDecimal totalBeforePoints, BigDecimal totalAfterPoints, int usedPoints) {}

    public CreateBookingResult createBooking(long userId, long screeningId, List<int[]> seatCoords, int pointsToUse) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            Screening screening = screeningRepo.findById(screeningId);
            if (screening == null) {
                conn.rollback();
                throw new IllegalArgumentException("Screening not found: " + screeningId);
            }

            User user = userRepo.findById(conn, userId);
            if (user == null) {
                conn.rollback();
                throw new IllegalArgumentException("User not found: " + userId);
            }

            List<Seat> seats = new ArrayList<>();
            for (int[] rc : seatCoords) {
                Seat seat = seatRepo.findByHallRowCol(conn, screening.hallId(), rc[0], rc[1]);
                if (seat == null) {
                    conn.rollback();
                    throw new IllegalArgumentException("Seat not found: row=" + rc[0] + " col=" + rc[1]);
                }
                seats.add(seat);
            }

            for (Seat seat : seats) {
                if (seatRepo.isSeatBookedForScreening(conn, screeningId, seat.id())) {
                    conn.rollback();
                    throw new IllegalStateException("Seat already booked: row=" + seat.rowNum() + " col=" + seat.colNum());
                }
            }

            BigDecimal total = BigDecimal.ZERO;
            List<BigDecimal> seatPrices = new ArrayList<>();
            for (Seat seat : seats) {
                BigDecimal p = pricing.seatPrice(screening.basePrice(), seat.seatType(), screening.startTime());
                seatPrices.add(p);
                total = total.add(p);
            }
            total = total.setScale(2, RoundingMode.HALF_UP);

            BigDecimal afterPoints = pricing.applyPointsDiscount(total, user.loyaltyPoints(), pointsToUse)
                    .setScale(2, RoundingMode.HALF_UP);

            int usedPoints = total.subtract(afterPoints).intValue(); // 1 point = 1 KZT

            long bookingId = bookingRepo.insertBooking(conn, userId, screeningId, afterPoints);

            for (int i = 0; i < seats.size(); i++) {
                bookingRepo.insertBookingItem(conn, bookingId, seats.get(i).id(), seatPrices.get(i));
            }

            if (usedPoints > 0) {
                userRepo.subtractPoints(conn, userId, usedPoints);
            }

            conn.commit();
            return new CreateBookingResult(bookingId, total, afterPoints, usedPoints);

        } catch (Exception e) {
            throw new RuntimeException("Create booking failed: " + e.getMessage(), e);
        }
    }

    public Booking pay(long bookingId) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            Booking b = bookingRepo.findById(conn, bookingId);
            if (b == null) {
                conn.rollback();
                throw new IllegalArgumentException("Booking not found: " + bookingId);
            }
            if (!"CREATED".equals(b.status())) {
                conn.rollback();
                throw new IllegalStateException("Only CREATED can be paid. Current=" + b.status());
            }

            bookingRepo.markPaid(conn, bookingId);

            int earned = pricing.earnedPoints(b.totalPrice());
            userRepo.addPoints(conn, b.userId(), earned);

            Booking updated = bookingRepo.findById(conn, bookingId);
            conn.commit();
            return updated;

        } catch (Exception e) {
            throw new RuntimeException("Pay failed: " + e.getMessage(), e);
        }
    }

    public Booking cancel(long bookingId) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            Booking b = bookingRepo.findById(conn, bookingId);
            if (b == null) {
                conn.rollback();
                throw new IllegalArgumentException("Booking not found: " + bookingId);
            }
            if ("CANCELLED".equals(b.status())) {
                conn.rollback();
                throw new IllegalStateException("Already cancelled");
            }

            Screening screening = screeningRepo.findById(b.screeningId());
            if (screening == null) {
                conn.rollback();
                throw new IllegalStateException("Screening not found for booking");
            }

            BigDecimal refund = computeRefund(b.totalPrice(), screening.startTime());
            bookingRepo.markCancelled(conn, bookingId, refund);

            Booking updated = bookingRepo.findById(conn, bookingId);
            conn.commit();
            return updated;

        } catch (Exception e) {
            throw new RuntimeException("Cancel failed: " + e.getMessage(), e);
        }
    }

    private BigDecimal computeRefund(BigDecimal total, LocalDateTime startTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(startTime)) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        long minutes = Duration.between(now, startTime).toMinutes();
        BigDecimal rate = (minutes >= 120) ? new BigDecimal("0.90") : new BigDecimal("0.50");
        return total.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
