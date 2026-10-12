package kz.jqd.arqa.service;

import kz.jqd.arqa.dto.NewTripRequest;
import kz.jqd.arqa.entity.Trip;
import kz.jqd.arqa.exception.TripConflictException;
import kz.jqd.arqa.model.AddTripResult;
import kz.jqd.arqa.repository.TripRepository;
import kz.jqd.arqa.service.interfaces.TripCreationService;
import kz.jqd.arqa.validation.TripValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** Adds trips idempotently: the same trip sent twice (even concurrently) is stored once. */
@Service
public class DefaultTripCreationService implements TripCreationService {

    private final TripValidator tripValidator;
    private final TripRepository tripRepository;

    public DefaultTripCreationService(TripValidator tripValidator, TripRepository tripRepository) {
        this.tripValidator = tripValidator;
        this.tripRepository = tripRepository;
    }

    /**
     * Not transactional on purpose: when two identical requests race, the loser hits the primary key,
     * and the following lookup must run in a fresh transaction to see the winner's row.
     */
    @Override
    public AddTripResult addTrip(NewTripRequest request) {
        tripValidator.validate(request);
        Trip candidate = toTrip(request);

        Trip existingTrip = tripRepository.findById(candidate.getTripIdentifier()).orElse(null);
        if (existingTrip != null) {
            return resolveExistingTrip(existingTrip, candidate);
        }

        try {
            return new AddTripResult(tripRepository.saveAndFlush(candidate), true);
        } catch (DataIntegrityViolationException exception) {
            Trip tripSavedByConcurrentRequest = tripRepository.findById(candidate.getTripIdentifier())
                    .orElseThrow(() -> exception);
            return resolveExistingTrip(tripSavedByConcurrentRequest, candidate);
        }
    }

    private AddTripResult resolveExistingTrip(Trip existingTrip, Trip candidate) {
        if (!existingTrip.hasSameDataAs(candidate)) {
            throw new TripConflictException(candidate.getTripIdentifier());
        }
        return new AddTripResult(existingTrip, false);
    }

    private Trip toTrip(NewTripRequest request) {
        return new Trip(request.tripIdentifier(), request.startedAt(), request.endedAt(),
                request.amount(), request.paymentMethod(), request.commission());
    }
}
