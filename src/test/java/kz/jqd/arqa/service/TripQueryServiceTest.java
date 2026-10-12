package kz.jqd.arqa.service;

import kz.jqd.arqa.IntegrationTest;
import kz.jqd.arqa.dto.NewTripRequest;
import kz.jqd.arqa.entity.Trip;
import kz.jqd.arqa.model.PaymentMethod;
import kz.jqd.arqa.repository.TripRepository;
import kz.jqd.arqa.service.interfaces.DailySummaryService;
import kz.jqd.arqa.service.interfaces.TripCreationService;
import kz.jqd.arqa.service.interfaces.TripQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class TripQueryServiceTest {

    @Autowired
    private TripCreationService tripCreationService;

    @Autowired
    private TripQueryService tripQueryService;

    @Autowired
    private DailySummaryService dailySummaryService;

    @Autowired
    private TripRepository tripRepository;

    @BeforeEach
    void removeAllTrips() {
        tripRepository.deleteAll();
    }

    @Test
    void returnsTripsOfTheDayEarliestFirst() {
        addTrip("late", "2026-10-01T18:00:00+05:00", "2026-10-01T18:30:00+05:00");
        addTrip("early", "2026-10-01T08:00:00+05:00", "2026-10-01T08:30:00+05:00");
        addTrip("other-day", "2026-10-02T08:00:00+05:00", "2026-10-02T08:30:00+05:00");

        assertThat(tripQueryService.findTripsByDate(LocalDate.of(2026, 10, 1)))
                .extracting(Trip::getTripIdentifier).containsExactly("early", "late");
    }

    @Test
    void tripCrossingMidnightIsCountedOnlyOnTheDayItStarted() {
        addTrip("late", "2026-10-02T23:50:00+05:00", "2026-10-03T00:15:00+05:00");

        assertThat(dailySummaryService.summarizeDay(LocalDate.of(2026, 10, 2)).tripCount()).isEqualTo(1);
        assertThat(dailySummaryService.summarizeDay(LocalDate.of(2026, 10, 3)).tripCount()).isZero();
    }

    @Test
    void listsDatesThatHaveTrips() {
        addTrip("a", "2026-10-04T08:00:00+05:00", "2026-10-04T08:30:00+05:00");
        addTrip("b", "2026-10-01T08:00:00+05:00", "2026-10-01T08:30:00+05:00");
        addTrip("c", "2026-10-01T09:00:00+05:00", "2026-10-01T09:30:00+05:00");

        assertThat(tripQueryService.findDatesWithTrips())
                .containsExactly(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 4));
    }

    private void addTrip(String tripIdentifier, String start, String end) {
        tripCreationService.addTrip(new NewTripRequest(tripIdentifier,
                OffsetDateTime.parse(start), OffsetDateTime.parse(end), 100L, PaymentMethod.CARD, 10L));
    }
}
