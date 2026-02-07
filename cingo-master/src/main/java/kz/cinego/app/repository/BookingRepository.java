package kz.cinego.app.repository;

import kz.cinego.app.entity.Booking;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;

public class BookingRepository {

    public long insertBooking(Connection conn, long userId, long screeningId, BigDecimal totalPrice) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO bookings(user_id, screening_id, status, total_price, created_at)
            VALUES(?, ?, 'CREATED', ?, ?)
        """, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, userId);
            ps.setLong(2, screeningId);
            ps.setBigDecimal(3, totalPrice);
            ps.setString(4, LocalDateTime.now().toString());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("No generated key for booking");
            }
        }
    }

    public void insertBookingItem(Connection conn, long bookingId, long seatId, BigDecimal price) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO booking_items(booking_id, seat_id, price) VALUES(?,?,?)")) {
            ps.setLong(1, bookingId);
            ps.setLong(2, seatId);
            ps.setBigDecimal(3, price);
            ps.executeUpdate();
        }
    }

    public Booking findById(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM bookings WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                LocalDateTime created = LocalDateTime.parse(rs.getString("created_at"));
                String paidStr = rs.getString("paid_at");
                String cancelledStr = rs.getString("cancelled_at");

                LocalDateTime paidAt = paidStr == null ? null : LocalDateTime.parse(paidStr);
                LocalDateTime cancelledAt = cancelledStr == null ? null : LocalDateTime.parse(cancelledStr);

                BigDecimal refund = rs.getBigDecimal("refund_amount");
                if (refund == null) refund = BigDecimal.ZERO;

                return new Booking(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getLong("screening_id"),
                        rs.getString("status"),
                        rs.getBigDecimal("total_price"),
                        created,
                        paidAt,
                        cancelledAt,
                        refund
                );
            }
        }
    }

    public void markPaid(Connection conn, long bookingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE bookings SET status='PAID', paid_at=? WHERE id=?")) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setLong(2, bookingId);
            ps.executeUpdate();
        }
    }

    public void markCancelled(Connection conn, long bookingId, BigDecimal refund) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE bookings SET status='CANCELLED', cancelled_at=?, refund_amount=? WHERE id=?")) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setBigDecimal(2, refund);
            ps.setLong(3, bookingId);
            ps.executeUpdate();
        }
    }
}
