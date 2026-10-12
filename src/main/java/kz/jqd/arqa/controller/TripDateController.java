package kz.jqd.arqa.controller;

import kz.jqd.arqa.service.interfaces.TripQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trip-dates")
public class TripDateController {

    private final TripQueryService tripQueryService;

    public TripDateController(TripQueryService tripQueryService) {
        this.tripQueryService = tripQueryService;
    }

    @GetMapping
    public List<LocalDate> listDatesWithTrips() {
        return tripQueryService.findDatesWithTrips();
    }
}
