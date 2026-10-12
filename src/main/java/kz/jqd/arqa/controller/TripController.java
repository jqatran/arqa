package kz.jqd.arqa.controller;

import kz.jqd.arqa.dto.NewTripRequest;
import kz.jqd.arqa.dto.PagedResponse;
import kz.jqd.arqa.dto.TripResponse;
import kz.jqd.arqa.model.AddTripResult;
import kz.jqd.arqa.model.DateRange;
import kz.jqd.arqa.service.interfaces.TripCreationService;
import kz.jqd.arqa.service.interfaces.TripQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private static final String DEFAULT_PAGE_SIZE = "50";

    private final TripCreationService tripCreationService;
    private final TripQueryService tripQueryService;

    public TripController(TripCreationService tripCreationService, TripQueryService tripQueryService) {
        this.tripCreationService = tripCreationService;
        this.tripQueryService = tripQueryService;
    }

    @GetMapping
    public List<TripResponse> listTrips(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return tripQueryService.findTripsByDate(date).stream().map(TripResponse::from).toList();
    }

    /** Trips of several days, paged. The range is limited to {@value DateRange#MAXIMUM_DAYS} days. */
    @GetMapping("/range")
    public PagedResponse<TripResponse> listTripsInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "page", defaultValue = "0") int pageIndex,
            @RequestParam(name = "size", defaultValue = DEFAULT_PAGE_SIZE) int pageSize) {
        return PagedResponse.from(
                tripQueryService.findTripsInRange(new DateRange(from, to), pageIndex, pageSize), TripResponse::from);
    }

    /**
     * 201 for a new trip, 200 when the very same trip was already stored (idempotent retry).
     * The trip may be identified by the body's {@code id} or by the {@code Idempotency-Key} header.
     */
    @PostMapping
    public ResponseEntity<TripResponse> addTrip(
            @RequestBody NewTripRequest request,
            @RequestHeader(name = NewTripRequest.IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey) {
        AddTripResult result = tripCreationService.addTrip(request.withIdempotencyKey(idempotencyKey));
        HttpStatus status = result.isNewlyCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(TripResponse.from(result.trip()));
    }
}
