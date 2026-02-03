package kz.cinego.app.controller;

import kz.cinego.app.entity.Booking;
import kz.cinego.app.service.BookingService;

import java.util.List;

public class BookingController {
    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    public BookingService.CreateBookingResult create(long userId, long screeningId, List<int[]> seats, int pointsToUse) {
        return service.createBooking(userId, screeningId, seats, pointsToUse);
    }

    public Booking pay(long bookingId) {
        return service.pay(bookingId);
    }

    public Booking cancel(long bookingId) {
        return service.cancel(bookingId);
    }
}
