package kz.cinego.app.ui;

import kz.cinego.app.controller.BookingController;
import kz.cinego.app.controller.MovieController;
import kz.cinego.app.controller.ScreeningController;
import kz.cinego.app.entity.Movie;
import kz.cinego.app.entity.Screening;
import kz.cinego.app.repository.UserRepository;
import kz.cinego.app.entity.User;

import java.util.*;

public class ConsoleUI {
    private final MovieController movieController;
    private final ScreeningController screeningController;
    private final BookingController bookingController;
    private final User defaultUser;

    public ConsoleUI(MovieController movieController,
                     ScreeningController screeningController,
                     BookingController bookingController) {
        this.movieController = movieController;
        this.screeningController = screeningController;
        this.bookingController = bookingController;
        this.defaultUser = new UserRepository().findDefaultUser();
    }

    public void run() {
        Scanner sc = new Scanner(System.in);

        System.out.println("=== CineGo (Console) ===");
        System.out.println("User: " + defaultUser.fullName() + " | points=" + defaultUser.loyaltyPoints());
        System.out.println();

        while (true) {
            System.out.println("""
                1) List movies
                2) List screenings by movie
                3) Create booking (seat check + pricing + points)
                4) Pay booking (earn points)
                5) Cancel booking (refund policy)
                0) Exit
                """);
            System.out.print("> ");

            String cmd = sc.nextLine().trim();

            try {
                switch (cmd) {
                    case "1" -> listMovies();
                    case "2" -> listScreenings(sc);
                    case "3" -> createBooking(sc);
                    case "4" -> pay(sc);
                    case "5" -> cancel(sc);
                    case "0" -> { System.out.println("Bye."); return; }
                    default -> System.out.println("Unknown command.");
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }

            System.out.println();
        }
    }

    private void listMovies() {
        List<Movie> movies = movieController.listMovies();
        System.out.println("Movies:");
        for (Movie m : movies) {
            System.out.printf("  [%d] %s (%s, %d min, %s)%n",
                    m.id(), m.title(), m.genre(), m.durationMin(), m.ageRating());
        }
    }

    private void listScreenings(Scanner sc) {
        System.out.print("movieId: ");
        long movieId = Long.parseLong(sc.nextLine().trim());

        List<Screening> list = screeningController.listByMovie(movieId);
        if (list.isEmpty()) {
            System.out.println("No screenings.");
            return;
        }

        for (Screening s : list) {
            System.out.printf("  [%d] start=%s hall=%d base=%s%n",
                    s.id(), s.startTime(), s.hallId(), s.basePrice());
        }
    }

    private void createBooking(Scanner sc) {
        System.out.print("screeningId: ");
        long screeningId = Long.parseLong(sc.nextLine().trim());

        System.out.println("Seats format: row,col row,col ...  (example: 1,1 1,2 6,8)");
        System.out.print("> ");
        String input = sc.nextLine().trim();
        List<int[]> seats = parseSeats(input);

        System.out.print("points to use (0 if none): ");
        int pointsToUse = Integer.parseInt(sc.nextLine().trim());

        var res = bookingController.create(defaultUser.id(), screeningId, seats, pointsToUse);

        System.out.println("Booking created: id=" + res.bookingId());
        System.out.println("Total before points: " + res.totalBeforePoints());
        System.out.println("Used points: " + res.usedPoints());
        System.out.println("Total after points: " + res.totalAfterPoints());
    }

    private void pay(Scanner sc) {
        System.out.print("bookingId: ");
        long bookingId = Long.parseLong(sc.nextLine().trim());

        var b = bookingController.pay(bookingId);
        System.out.println("PAID. total=" + b.totalPrice() + " paidAt=" + b.paidAt());
    }

    private void cancel(Scanner sc) {
        System.out.print("bookingId: ");
        long bookingId = Long.parseLong(sc.nextLine().trim());

        var b = bookingController.cancel(bookingId);
        System.out.println("CANCELLED. refund=" + b.refundAmount());
    }

    private List<int[]> parseSeats(String input) {
        if (input.isEmpty()) throw new IllegalArgumentException("No seats provided.");

        String[] parts = input.split("\\s+");
        List<int[]> list = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String p : parts) {
            String[] rc = p.split(",");
            if (rc.length != 2) throw new IllegalArgumentException("Bad seat: " + p);

            int r = Integer.parseInt(rc[0]);
            int c = Integer.parseInt(rc[1]);

            String key = r + ":" + c;
            if (seen.add(key)) list.add(new int[]{r, c});
        }

        return list;
    }
}
