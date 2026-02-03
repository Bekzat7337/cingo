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

    public BookingService(
            ScreeningRepository screeningRepo,
            SeatRepository seatRepo,
            BookingRepository bookingRepo,
            UserRepository userRepo,
            PricingService pricing
    ) {
        this.screeningRepo = screeningRepo;
        this.seatRepo = seatRepo;
        this.bookingRepo = bookingRepo;
        this.userRepo = userRepo;
        this.pricing = pricing;
    }

    public record CreateBookingResult(
            long bookingId,
            BigDecimal totalBeforePoints,
            BigDecimal totalAfterPoints,
            int usedPoints
    ) {}

    public CreateBookingResult createBooking(
            long userId,
            long screeningId,
            List<int[]> seatCoords,
            int pointsToUse
    ) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            Screening screening = getScreening(screeningId);
            User user = getUser(conn, userId);

            List<Seat> seats = getSeats(conn, screening, seatCoords);
            checkSeatsAvailability(conn, screeningId, seats);

            List<BigDecimal> seatPrices = new ArrayList<>();
            BigDecimal total = calculateTotal(screening, seats, seatPrices);

            BigDecimal afterPoints = pricing
                    .applyPointsDiscount(total, user.loyaltyPoints(), pointsToUse)
                    .setScale(2, RoundingMode.HALF_UP);

            int usedPoints = total.subtract(afterPoints).intValue();

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

            Booking booking = bookingRepo.findById(conn, bookingId);
            if (booking == null) {
                conn.rollback();
                throw new IllegalArgumentException("Booking not found: " + bookingId);
            }
            if (!"CREATED".equals(booking.status())) {
                conn.rollback();
                throw new IllegalStateException("Only CREATED can be paid. Current=" + booking.status());
            }

            bookingRepo.markPaid(conn, bookingId);

            int earned = pricing.earnedPoints(booking.totalPrice());
            userRepo.addPoints(conn, booking.userId(), earned);

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

            Booking booking = bookingRepo.findById(conn, bookingId);
            if (booking == null) {
                conn.rollback();
                throw new IllegalArgumentException("Booking not found: " + bookingId);
            }
            if ("CANCELLED".equals(booking.status())) {
                conn.rollback();
                throw new IllegalStateException("Already cancelled");
            }

            Screening screening = getScreening(booking.screeningId());

            BigDecimal refund = computeRefund(booking.totalPrice(), screening.startTime());
            bookingRepo.markCancelled(conn, bookingId, refund);

            Booking updated = bookingRepo.findById(conn, bookingId);
            conn.commit();
            return updated;

        } catch (Exception e) {
            throw new RuntimeException("Cancel failed: " + e.getMessage(), e);
        }
    }

    private Screening getScreening(long id) {
        Screening screening = screeningRepo.findById(id);
        if (screening == null) {
            throw new IllegalArgumentException("Screening not found: " + id);
        }
        return screening;
    }

    private User getUser(Connection conn, long id) {
        User user = userRepo.findById(conn, id);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        return user;
    }

    private List<Seat> getSeats(Connection conn, Screening screening, List<int[]> seatCoords) {
        List<Seat> seats = new ArrayList<>();
        for (int[] rc : seatCoords) {
            Seat seat = seatRepo.findByHallRowCol(conn, screening.hallId(), rc[0], rc[1]);
            if (seat == null) {
                throw new IllegalArgumentException("Seat not found: row=" + rc[0] + " col=" + rc[1]);
            }
            seats.add(seat);
        }
        return seats;
    }

    private void checkSeatsAvailability(Connection conn, long screeningId, List<Seat> seats) {
        for (Seat seat : seats) {
            if (seatRepo.isSeatBookedForScreening(conn, screeningId, seat.id())) {
                throw new IllegalStateException(
                        "Seat already booked: row=" + seat.rowNum() + " col=" + seat.colNum()
                );
            }
        }
    }

    private BigDecimal calculateTotal(
            Screening screening,
            List<Seat> seats,
            List<BigDecimal> seatPrices
    ) {
        BigDecimal total = BigDecimal.ZERO;

        for (Seat seat : seats) {
            BigDecimal price = pricing.seatPrice(
                    screening.basePrice(),
                    seat.seatType(),
                    screening.startTime()
            );
            seatPrices.add(price);
            total = total.add(price);
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeRefund(BigDecimal total, LocalDateTime startTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(startTime)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long minutes = Duration.between(now, startTime).toMinutes();
        BigDecimal rate = minutes >= 120
                ? new BigDecimal("0.90")
                : new BigDecimal("0.50");

        return total.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
