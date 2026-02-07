package kz.cinego.app.repository;

import kz.cinego.app.db.Database;
import kz.cinego.app.entity.User;

import java.sql.*;

public class UserRepository {

    public User findDefaultUser() {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM users ORDER BY id LIMIT 1");
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) return null;
            return new User(
                    rs.getLong("id"),
                    rs.getString("full_name"),
                    rs.getString("phone"),
                    rs.getString("role"),
                    rs.getInt("loyalty_points")
            );
        } catch (SQLException e) {
            throw new RuntimeException("UserRepository.findDefaultUser failed: " + e.getMessage(), e);
        }
    }

    public User findById(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new User(
                        rs.getLong("id"),
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        rs.getString("role"),
                        rs.getInt("loyalty_points")
                );
            }
        }
    }

    public void addPoints(Connection conn, long userId, int add) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET loyalty_points = loyalty_points + ? WHERE id=?")) {
            ps.setInt(1, add);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    public void subtractPoints(Connection conn, long userId, int sub) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET loyalty_points = MAX(loyalty_points - ?, 0) WHERE id=?")) {
            ps.setInt(1, sub);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }
}
