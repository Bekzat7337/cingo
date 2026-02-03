package kz.cinego.app.config;

public final class DbConfig {

    public static final String JDBC_URL = "jdbc:sqlite:cinema.db";

    private DbConfig() {
        throw new AssertionError("Cannot instantiate utility class");
    }
}