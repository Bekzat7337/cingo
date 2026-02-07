package kz.cinego.app.repository;

import kz.cinego.app.db.Database;
import kz.cinego.app.entity.Screening;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScreeningRepository {

    public List<Screening> findByMovie(long movieId) {
        List<Screening> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM screenings WHERE movie_id=? ORDER BY start_time")) {
            ps.setLong(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("ScreeningRepository.findByMovie failed: " + e.getMessage(), e);
        }
    }

    public Screening findById(long id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM screenings WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("ScreeningRepository.findById failed: " + e.getMessage(), e);
        }
    }

    private Screening map(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long movieId = rs.getLong("movie_id");
        long hallId = rs.getLong("hall_id");
        LocalDateTime start = LocalDateTime.parse(rs.getString("start_time"));
        BigDecimal base = rs.getBigDecimal("base_price");
        return new Screening(id, movieId, hallId, start, base);
    }
}
