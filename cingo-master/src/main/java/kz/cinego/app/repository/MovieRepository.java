package kz.cinego.app.repository;

import kz.cinego.app.db.Database;
import kz.cinego.app.entity.Movie;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MovieRepository {

    public List<Movie> findAll() {
        List<Movie> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM movies ORDER BY id");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Movie(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("genre"),
                        rs.getInt("duration_min"),
                        rs.getString("age_rating")
                ));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("MovieRepository.findAll failed: " + e.getMessage(), e);
        }
    }

    public Movie findById(long id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM movies WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Movie(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("genre"),
                        rs.getInt("duration_min"),
                        rs.getString("age_rating")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("MovieRepository.findById failed: " + e.getMessage(), e);
        }
    }
}
