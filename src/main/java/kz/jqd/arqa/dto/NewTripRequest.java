package kz.jqd.arqa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import kz.jqd.arqa.exception.InvalidTripException;
import kz.jqd.arqa.model.FieldViolation;
import kz.jqd.arqa.model.PaymentMethod;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Incoming trip. JSON names follow the data format of the task ({@code id, start, end, amount, payment, commission});
 * all fields are nullable here so that the validator can report every problem in one response.
 */
public record NewTripRequest(
        @JsonProperty("id") String tripIdentifier,
        @JsonProperty("start") OffsetDateTime startedAt,
        @JsonProperty("end") OffsetDateTime endedAt,
        @JsonProperty("amount") Long amount,
        @JsonProperty("payment") PaymentMethod paymentMethod,
        @JsonProperty("commission") Long commission) {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    /**
     * Applies the {@code Idempotency-Key} header: it names the trip when the body has no {@code id},
     * and must equal the {@code id} when both are given. Blank or absent header changes nothing.
     *
     * @throws InvalidTripException if the header and the body identify different trips
     */
    public NewTripRequest withIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return this;
        }
        if (tripIdentifier == null || tripIdentifier.isBlank()) {
            return new NewTripRequest(idempotencyKey, startedAt, endedAt, amount, paymentMethod, commission);
        }
        if (!tripIdentifier.equals(idempotencyKey)) {
            throw new InvalidTripException(List.of(
                    new FieldViolation(IDEMPOTENCY_KEY_HEADER, "trip.idempotencyKey.mismatch")));
        }
        return this;
    }
}
