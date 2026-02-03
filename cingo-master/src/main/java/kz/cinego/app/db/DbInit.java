package kz.cinego.app.db;

import java.math.BigDecimal;
import java.sql.*;

public final class DbInit {
    private DbInit() {}

    public static void init() {
        try (Connection conn = Database.getConnection()) {
            createTables(conn);
            seedIfEmpty(conn);
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed: " + e.getMessage(), e);
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS movies (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  title TEXT NOT NULL,
                  genre TEXT,
                  duration_min INTEGER NOT NULL,
                  age_rating TEXT
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS halls (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  name TEXT NOT NULL,
                  rows_count INTEGER NOT NULL,
                  cols_count INTEGER NOT NULL
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS seats (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  hall_id INTEGER NOT NULL,
                  row_num INTEGER NOT NULL,
                  col_num INTEGER NOT NULL,
                  seat_type TEXT NOT NULL,
                  UNIQUE(hall_id, row_num, col_num),
                  FOREIGN KEY (hall_id) REFERENCES halls(id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS screenings (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  movie_id INTEGER NOT NULL,
                  hall_id INTEGER NOT NULL,
                  start_time TEXT NOT NULL,
                  base_price NUMERIC NOT NULL,
                  FOREIGN KEY (movie_id) REFERENCES movies(id),
                  FOREIGN KEY (hall_id) REFERENCES halls(id)
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  full_name TEXT NOT NULL,
                  phone TEXT,
                  role TEXT NOT NULL,
                  loyalty_points INTEGER NOT NULL DEFAULT 0
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS bookings (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  user_id INTEGER NOT NULL,
                  screening_id INTEGER NOT NULL,
                  status TEXT NOT NULL,
                  total_price NUMERIC NOT NULL,
                  created_at TEXT NOT NULL,
                  paid_at TEXT,
                  cancelled_at TEXT,
                  refund_amount NUMERIC DEFAULT 0,
                  FOREIGN KEY (user_id) REFERENCES users(id),
                  FOREIGN KEY (screening_id) REFERENCES screenings(id)
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS booking_items (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  booking_id INTEGER NOT NULL,
                  seat_id INTEGER NOT NULL,
                  price NUMERIC NOT NULL,
                  UNIQUE(booking_id, seat_id),
                  FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
                  FOREIGN KEY (seat_id) REFERENCES seats(id)
                );
            """);
        }
    }

    private static void seedIfEmpty(Connection conn) throws SQLException {
        if (isEmpty(conn, "movies")) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO movies(title, genre, duration_min, age_rating) VALUES(?,?,?,?)")) {
                addMovie(ps, "Dune: Part Two", "Sci-Fi", 166, "PG-13");
                addMovie(ps, "Interstellar", "Sci-Fi", 169, "PG-13");
                addMovie(ps, "Spider-Man: Into the Spider-Verse", "Animation", 117, "PG");
            }
        }

        if (isEmpty(conn, "halls")) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO halls(name, rows_count, cols_count) VALUES(?,?,?)")) {
                ps.setString(1, "Hall A");
                ps.setInt(2, 6);
                ps.setInt(3, 8);
                ps.executeUpdate();
            }
        }

        if (isEmpty(conn, "seats")) {
            long hallId = firstId(conn, "halls");
            int rows = getInt(conn, "SELECT rows_count FROM halls WHERE id=?", hallId);
            int cols = getInt(conn, "SELECT cols_count FROM halls WHERE id=?", hallId);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO seats(hall_id, row_num, col_num, seat_type) VALUES(?,?,?,?)")) {
                for (int r = 1; r <= rows; r++) {
                    for (int c = 1; c <= cols; c++) {
                        ps.setLong(1, hallId);
                        ps.setInt(2, r);
                        ps.setInt(3, c);
                        ps.setString(4, (r >= rows - 1) ? "VIP" : "STANDARD");
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
        }

        if (isEmpty(conn, "screenings")) {
            long movieId = firstId(conn, "movies");
            long hallId = firstId(conn, "halls");

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO screenings(movie_id, hall_id, start_time, base_price) VALUES(?,?,?,?)")) {

                ps.setLong(1, movieId);
                ps.setLong(2, hallId);
                ps.setString(3, "2026-02-05T19:30");
                ps.setBigDecimal(4, new BigDecimal("1500.00"));
                ps.executeUpdate();

                ps.setLong(1, movieId);
                ps.setLong(2, hallId);
                ps.setString(3, "2026-02-05T14:00");
                ps.setBigDecimal(4, new BigDecimal("1200.00"));
                ps.executeUpdate();
            }
        }

        if (isEmpty(conn, "users")) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users(full_name, phone, role, loyalty_points) VALUES(?,?,?,?)")) {
                ps.setString(1, "Default User");
                ps.setString(2, "+7 700 000 00 00");
                ps.setString(3, "CUSTOMER");
                ps.setInt(4, 0);
                ps.executeUpdate();
            }
        }
    }

    private static boolean isEmpty(Connection conn, String table) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    private static void addMovie(PreparedStatement ps, String title, String genre, int duration, String rating) throws SQLException {
        ps.setString(1, title);
        ps.setString(2, genre);
        ps.setInt(3, duration);
        ps.setString(4, rating);
        ps.executeUpdate();
    }

    private static long firstId(Connection conn, String table) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM " + table + " ORDER BY id LIMIT 1")) {
            if (rs.next()) return rs.getLong(1);
            throw new SQLException("No rows in " + table);
        }
    }

    private static int getInt(Connection conn, String sql, long param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("No result");
                return rs.getInt(1);
            }
        }
    }
}
