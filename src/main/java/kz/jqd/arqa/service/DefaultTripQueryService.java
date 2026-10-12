package kz.jqd.arqa.service;

import kz.jqd.arqa.entity.Trip;
import kz.jqd.arqa.exception.InvalidQueryException;
import kz.jqd.arqa.model.DateRange;
import kz.jqd.arqa.repository.TripRepository;
import kz.jqd.arqa.service.interfaces.TripQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DefaultTripQueryService implements TripQueryService {

    private final TripRepository tripRepository;

    public DefaultTripQueryService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @Override
    public List<Trip> findTripsByDate(LocalDate date) {
        return tripRepository.findByLocalTripDateOrderByStartedAtAsc(date);
    }

    @Override
    public List<LocalDate> findDatesWithTrips() {
        return tripRepository.findAllDatesWithTrips();
    }

    @Override
    public List<Trip> findAllTripsInRange(DateRange range) {
        return tripRepository.findByLocalTripDateBetweenOrderByStartedAtAsc(range.from(), range.to());
    }

    @Override
    public Page<Trip> findTripsInRange(DateRange range, int pageIndex, int pageSize) {
        if (pageIndex < 0) {
            throw new InvalidQueryException("query.page.negative");
        }
        if (pageSize < 1 || pageSize > MAXIMUM_PAGE_SIZE) {
            throw new InvalidQueryException("query.size.outOfRange", MAXIMUM_PAGE_SIZE);
        }
        // The identifier is a tie-breaker so that pages stay stable when two trips start at the same moment.
        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize, Sort.by("startedAt", "tripIdentifier"));
        return tripRepository.findByLocalTripDateBetween(range.from(), range.to(), pageRequest);
    }
}
