package kz.cinego.app.entity;

public record Seat(long id, long hallId, int rowNum, int colNum, String seatType) {}
