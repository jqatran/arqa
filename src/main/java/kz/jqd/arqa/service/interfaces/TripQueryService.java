package kz.jqd.arqa.service.interfaces;

import kz.jqd.arqa.entity.Trip;
import kz.jqd.arqa.exception.InvalidQueryException;
import kz.jqd.arqa.model.DateRange;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

/** Read side: which trips happened when, and on which days there are any. */
public interface TripQueryService {

    int MAXIMUM_PAGE_SIZE = 200;

    /** Trips that started on the given driver-local day, earliest first. */
    List<Trip> findTripsByDate(LocalDate date);

    /** All days that have at least one trip, oldest first. */
    List<LocalDate> findDatesWithTrips();

    /** Every trip that started within the range, earliest first (bounded by the range length). */
    List<Trip> findAllTripsInRange(DateRange range);

    /**
     * One page of trips that started within the range, earliest first.
     *
     * @param pageIndex zero-based page number
     * @throws InvalidQueryException for a negative page or a size outside 1..{@value #MAXIMUM_PAGE_SIZE}
     */
    Page<Trip> findTripsInRange(DateRange range, int pageIndex, int pageSize);
}
