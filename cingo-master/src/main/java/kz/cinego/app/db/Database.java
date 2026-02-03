package kz.cinego.app.db;

import kz.cinego.app.config.DbConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private Database() {}

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DbConfig.JDBC_URL);
        conn.createStatement().execute("PRAGMA foreign_keys = ON;");
        return conn;
    }
}
