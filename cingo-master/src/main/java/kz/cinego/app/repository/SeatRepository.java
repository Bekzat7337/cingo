package kz.cinego.app.repository;

import kz.cinego.app.entity.Seat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SeatRepository {

    public Seat findByHallRowCol(Connection conn, long hallId, int row, int col) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM seats WHERE hall_id=? AND row_num=? AND col_num=?")) {
            ps.setLong(1, hallId);
            ps.setInt(2, row);
            ps.setInt(3, col);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Seat(
                        rs.getLong("id"),
                        rs.getLong("hall_id"),
                        rs.getInt("row_num"),
                        rs.getInt("col_num"),
                        rs.getString("seat_type")
                );
            }
        }
    }

    public boolean isSeatBookedForScreening(Connection conn, long screeningId, long seatId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT COUNT(*)
            FROM booking_items bi
            JOIN bookings b ON b.id = bi.booking_id
            WHERE b.screening_id = ?
              AND bi.seat_id = ?
              AND b.status IN ('CREATED','PAID')
        """)) {
            ps.setLong(1, screeningId);
            ps.setLong(2, seatId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
