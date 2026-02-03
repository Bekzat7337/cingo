package kz.cinego.app.ui;

import kz.cinego.app.controller.BookingController;
import kz.cinego.app.controller.MovieController;
import kz.cinego.app.controller.ScreeningController;
import kz.cinego.app.entity.Movie;
import kz.cinego.app.entity.Screening;
import kz.cinego.app.entity.User;
import kz.cinego.app.repository.UserRepository;

import java.util.*;

public class ConsoleUI {

    private final MovieController movieController;
    private final ScreeningController screeningController;
    private final BookingController bookingController;
    private final User currentUser;

    public ConsoleUI(
            MovieController movieController,
            ScreeningController screeningController,
            BookingController bookingController
    ) {
        this.movieController = movieController;
        this.screeningController = screeningController;
        this.bookingController = bookingController;
        this.currentUser = new UserRepository().findDefaultUser();
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== CineGo (Console) ===");
        System.out.println(
                "User: " + currentUser.fullName() +
                        " | points=" + currentUser.loyaltyPoints()
        );
        System.out.println();

        while (true) {
            printMenu();
            System.out.print("> ");

            String command = scanner.nextLine().trim();

            try {
                switch (command) {
                    case "1" -> showMovies();
                    case "2" -> showScreenings(scanner);
                    case "3" -> handleCreateBooking(scanner);
                    case "4" -> handlePay(scanner);
                    case "5" -> handleCancel(scanner);
                    case "0" -> {
                        System.out.println("Bye.");
                        return;
                    }
                    default -> System.out.println("Unknown command.");
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }

            System.out.println();
        }
    }

    private void printMenu() {
        System.out.println("""
                1) List movies
                2) List screenings by movie
                3) Create booking (seat check + pricing + points)
                4) Pay booking (earn points)
                5) Cancel booking (refund policy)
                0) Exit
                """);
    }

    private void showMovies() {
        List<Movie> movies = movieController.listMovies();
        System.out.println("Movies:");

        for (Movie movie : movies) {
            System.out.printf(
                    "  [%d] %s (%s, %d min, %s)%n",
                    movie.id(),
                    movie.title(),
                    movie.genre(),
                    movie.durationMin(),
                    movie.ageRating()
            );
        }
    }

    private void showScreenings(Scanner scanner) {
        System.out.print("movieId: ");
        long movieId = Long.parseLong(scanner.nextLine().trim());

        List<Screening> screenings = screeningController.listByMovie(movieId);
        if (screenings.isEmpty()) {
            System.out.println("No screenings.");
            return;
        }

        for (Screening screening : screenings) {
            System.out.printf(
                    "  [%d] start=%s hall=%d base=%s%n",
                    screening.id(),
                    screening.startTime(),
                    screening.hallId(),
                    screening.basePrice()
            );
        }
    }

    private void handleCreateBooking(Scanner scanner) {
        System.out.print("screeningId: ");
        long screeningId = Long.parseLong(scanner.nextLine().trim());

        System.out.println("Seats format: row,col row,col ...  (example: 1,1 1,2 6,8)");
        System.out.print("> ");
        String input = scanner.nextLine().trim();

        List<int[]> seats = parseSeats(input);

        System.out.print("points to use (0 if none): ");
        int pointsToUse = Integer.parseInt(scanner.nextLine().trim());

        var result = bookingController.create(
                currentUser.id(),
                screeningId,
                seats,
                pointsToUse
        );

        System.out.println("Booking created: id=" + result.bookingId());
        System.out.println("Total before points: " + result.totalBeforePoints());
        System.out.println("Used points: " + result.usedPoints());
        System.out.println("Total after points: " + result.totalAfterPoints());
    }

    private void handlePay(Scanner scanner) {
        System.out.print("bookingId: ");
        long bookingId = Long.parseLong(scanner.nextLine().trim());

        var booking = bookingController.pay(bookingId);
        System.out.println(
                "PAID. total=" + booking.totalPrice() +
                        " paidAt=" + booking.paidAt()
        );
    }

    private void handleCancel(Scanner scanner) {
        System.out.print("bookingId: ");
        long bookingId = Long.parseLong(scanner.nextLine().trim());

        var booking = bookingController.cancel(bookingId);
        System.out.println("CANCELLED. refund=" + booking.refundAmount());
    }

    private List<int[]> parseSeats(String input) {
        if (input.isEmpty()) {
            throw new IllegalArgumentException("No seats provided.");
        }

        String[] parts = input.split("\\s+");
        List<int[]> seats = new ArrayList<>();
        Set<String> unique = new HashSet<>();

        for (String part : parts) {
            String[] rc = part.split(",");
            if (rc.length != 2) {
                throw new IllegalArgumentException("Bad seat: " + part);
            }

            int row = Integer.parseInt(rc[0]);
            int col = Integer.parseInt(rc[1]);

            String key = row + ":" + col;
            if (unique.add(key)) {
                seats.add(new int[]{row, col});
            }
        }

        return seats;
    }
}
