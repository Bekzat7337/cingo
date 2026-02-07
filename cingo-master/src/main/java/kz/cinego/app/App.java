package kz.cinego.app;

import kz.cinego.app.controller.BookingController;
import kz.cinego.app.controller.MovieController;
import kz.cinego.app.controller.ScreeningController;
import kz.cinego.app.db.DbInit;
import kz.cinego.app.repository.*;
import kz.cinego.app.service.BookingService;
import kz.cinego.app.service.PricingService;
import kz.cinego.app.ui.ConsoleUI;

public class App {
    public static void main(String[] args) {
        DbInit.init(); // create tables + seed data

        MovieRepository movieRepo = new MovieRepository();
        ScreeningRepository screeningRepo = new ScreeningRepository();
        SeatRepository seatRepo = new SeatRepository();
        BookingRepository bookingRepo = new BookingRepository();
        UserRepository userRepo = new UserRepository();

        PricingService pricingService = new PricingService();
        BookingService bookingService = new BookingService(
                screeningRepo, seatRepo, bookingRepo, userRepo, pricingService
        );

        MovieController movieController = new MovieController(movieRepo);
        ScreeningController screeningController = new ScreeningController(screeningRepo);
        BookingController bookingController = new BookingController(bookingService);

        new ConsoleUI(movieController, screeningController, bookingController).run();
    }
}
