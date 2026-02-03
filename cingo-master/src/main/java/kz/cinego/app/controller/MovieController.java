package kz.cinego.app.controller;

import kz.cinego.app.entity.Movie;
import kz.cinego.app.repository.MovieRepository;

import java.util.List;

public class MovieController {
    private final MovieRepository repo;

    public MovieController(MovieRepository repo) {
        this.repo = repo;
    }

    public List<Movie> listMovies() {
        return repo.findAll();
    }
}
