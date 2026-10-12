package kz.jqd.arqa.service;

import kz.jqd.arqa.entity.Trip;
import kz.jqd.arqa.model.DailySummary;
import kz.jqd.arqa.model.DateRange;
import kz.jqd.arqa.model.PeriodSummary;
import kz.jqd.arqa.service.interfaces.DailySummaryService;
import kz.jqd.arqa.service.interfaces.TripQueryService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class DefaultDailySummaryService implements DailySummaryService {

    private final TripQueryService tripQueryService;
    private final DailySummaryCalculator dailySummaryCalculator;

    public DefaultDailySummaryService(TripQueryService tripQueryService, DailySummaryCalculator dailySummaryCalculator) {
        this.tripQueryService = tripQueryService;
        this.dailySummaryCalculator = dailySummaryCalculator;
    }

    @Override
    public DailySummary summarizeDay(LocalDate date) {
        return dailySummaryCalculator.calculate(date, tripQueryService.findTripsByDate(date));
    }

    @Override
    public PeriodSummary summarizePeriod(DateRange range) {
        List<Trip> tripsInRange = tripQueryService.findAllTripsInRange(range);

        Map<LocalDate, List<Trip>> tripsByDay = tripsInRange.stream()
                .collect(Collectors.groupingBy(Trip::getLocalTripDate, TreeMap::new, Collectors.toList()));
        List<DailySummary> days = tripsByDay.entrySet().stream()
                .map(dayAndTrips -> dailySummaryCalculator.calculate(dayAndTrips.getKey(), dayAndTrips.getValue()))
                .toList();

        // The total carries the first day of the range as its date; only PeriodSummary.range is meaningful to clients.
        DailySummary total = dailySummaryCalculator.calculate(range.from(), tripsInRange);
        return new PeriodSummary(range, total, days);
    }
}
