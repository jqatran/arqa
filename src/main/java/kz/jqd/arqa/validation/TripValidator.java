package kz.jqd.arqa.validation;

import kz.jqd.arqa.dto.NewTripRequest;
import kz.jqd.arqa.exception.InvalidTripException;
import kz.jqd.arqa.model.FieldViolation;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Business rules for a new trip. Collects every violation instead of stopping at the first one.
 * Field names in violations are the JSON names, so API clients can attach messages to their inputs.
 */
@Component
public class TripValidator {

    static final int MAXIMUM_TRIP_IDENTIFIER_LENGTH = 100;
    static final long MAXIMUM_AMOUNT = 10_000_000L;
    static final Duration MAXIMUM_TRIP_DURATION = Duration.ofHours(24);

    private static final String IDENTIFIER_FIELD = "id";
    private static final String START_FIELD = "start";
    private static final String END_FIELD = "end";
    private static final String AMOUNT_FIELD = "amount";
    private static final String PAYMENT_FIELD = "payment";
    private static final String COMMISSION_FIELD = "commission";

    /** @throws InvalidTripException listing all broken rules */
    public void validate(NewTripRequest request) {
        List<FieldViolation> violations = new ArrayList<>();

        checkIdentifier(request, violations);
        checkTimes(request, violations);
        checkAmount(request, violations);
        checkPaymentMethod(request, violations);
        checkCommission(request, violations);

        if (!violations.isEmpty()) {
            throw new InvalidTripException(violations);
        }
    }

    private void checkIdentifier(NewTripRequest request, List<FieldViolation> violations) {
        String tripIdentifier = request.tripIdentifier();
        if (tripIdentifier == null || tripIdentifier.isBlank()) {
            violations.add(new FieldViolation(IDENTIFIER_FIELD, "trip.id.blank"));
        } else if (tripIdentifier.length() > MAXIMUM_TRIP_IDENTIFIER_LENGTH) {
            violations.add(new FieldViolation(IDENTIFIER_FIELD,
                    "trip.id.tooLong", List.of(MAXIMUM_TRIP_IDENTIFIER_LENGTH)));
        }
    }

    /** Start and end are compared as instants, not as clock readings, because their offsets may differ. */
    private void checkTimes(NewTripRequest request, List<FieldViolation> violations) {
        if (request.startedAt() == null) {
            violations.add(new FieldViolation(START_FIELD, "trip.start.required"));
        }
        if (request.endedAt() == null) {
            violations.add(new FieldViolation(END_FIELD, "trip.end.required"));
        }
        boolean areBothTimesPresent = request.startedAt() != null && request.endedAt() != null;
        if (areBothTimesPresent && !request.endedAt().toInstant().isAfter(request.startedAt().toInstant())) {
            violations.add(new FieldViolation(END_FIELD, "trip.end.beforeStart"));
        } else if (areBothTimesPresent
                && Duration.between(request.startedAt(), request.endedAt()).compareTo(MAXIMUM_TRIP_DURATION) > 0) {
            violations.add(new FieldViolation(END_FIELD,
                    "trip.end.tooLong", List.of(MAXIMUM_TRIP_DURATION.toHours())));
        }
    }

    private void checkAmount(NewTripRequest request, List<FieldViolation> violations) {
        if (request.amount() == null) {
            violations.add(new FieldViolation(AMOUNT_FIELD, "trip.amount.required"));
        } else if (request.amount() <= 0) {
            violations.add(new FieldViolation(AMOUNT_FIELD, "trip.amount.notPositive"));
        } else if (request.amount() > MAXIMUM_AMOUNT) {
            violations.add(new FieldViolation(AMOUNT_FIELD, "trip.amount.tooLarge", List.of(MAXIMUM_AMOUNT)));
        }
    }

    private void checkPaymentMethod(NewTripRequest request, List<FieldViolation> violations) {
        if (request.paymentMethod() == null) {
            violations.add(new FieldViolation(PAYMENT_FIELD, "trip.payment.required"));
        }
    }

    private void checkCommission(NewTripRequest request, List<FieldViolation> violations) {
        if (request.commission() == null) {
            violations.add(new FieldViolation(COMMISSION_FIELD, "trip.commission.required"));
        } else if (request.commission() < 0) {
            violations.add(new FieldViolation(COMMISSION_FIELD, "trip.commission.negative"));
        } else if (request.amount() != null && request.commission() > request.amount()) {
            violations.add(new FieldViolation(COMMISSION_FIELD, "trip.commission.exceedsAmount"));
        }
    }
}
