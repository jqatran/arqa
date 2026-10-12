package kz.jqd.arqa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import kz.jqd.arqa.entity.Trip;
import kz.jqd.arqa.model.PaymentMethod;

import java.time.OffsetDateTime;

/** Trip as returned by the API; same JSON names as the input data format. */
public record TripResponse(
        @JsonProperty("id") String tripIdentifier,
        @JsonProperty("start") OffsetDateTime startedAt,
        @JsonProperty("end") OffsetDateTime endedAt,
        @JsonProperty("amount") long amount,
        @JsonProperty("payment") PaymentMethod paymentMethod,
        @JsonProperty("commission") long commission) {

    public static TripResponse from(Trip trip) {
        return new TripResponse(trip.getTripIdentifier(), trip.getStartedAt(), trip.getEndedAt(),
                trip.getAmount(), trip.getPaymentMethod(), trip.getCommission());
    }
}
