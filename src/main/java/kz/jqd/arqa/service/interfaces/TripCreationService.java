package kz.jqd.arqa.service.interfaces;

import kz.jqd.arqa.dto.NewTripRequest;
import kz.jqd.arqa.exception.InvalidTripException;
import kz.jqd.arqa.exception.TripConflictException;
import kz.jqd.arqa.model.AddTripResult;

/** Write side: registers trips. Adding the same trip again must not create a second record. */
public interface TripCreationService {

    /**
     * Stores the trip unless the same trip (same identifier and same data) is already stored.
     *
     * @throws InvalidTripException  if the data breaks the rules
     * @throws TripConflictException if the identifier exists with different data
     */
    AddTripResult addTrip(NewTripRequest request);
}
