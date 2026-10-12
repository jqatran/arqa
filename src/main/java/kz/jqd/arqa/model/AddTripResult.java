package kz.jqd.arqa.model;

import kz.jqd.arqa.entity.Trip;

/** Outcome of adding a trip: {@code isNewlyCreated} is false when the same trip was already stored. */
public record AddTripResult(Trip trip, boolean isNewlyCreated) {
}
