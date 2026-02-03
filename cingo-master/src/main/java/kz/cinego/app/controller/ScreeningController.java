package kz.cinego.app.controller;

import kz.cinego.app.entity.Screening;
import kz.cinego.app.repository.ScreeningRepository;

import java.util.List;

public class ScreeningController {
    private final ScreeningRepository repo;

    public ScreeningController(ScreeningRepository repo) {
        this.repo = repo;
    }

    public List<Screening> listByMovie(long movieId) {
        return repo.findByMovie(movieId);
    }
}
