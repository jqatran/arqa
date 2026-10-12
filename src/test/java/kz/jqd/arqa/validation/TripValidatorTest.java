package kz.jqd.arqa.validation;

import kz.jqd.arqa.dto.NewTripRequest;
import kz.jqd.arqa.exception.InvalidTripException;
import kz.jqd.arqa.model.FieldViolation;
import kz.jqd.arqa.model.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripValidatorTest {

    private static final String START = "2026-10-01T08:10:00+05:00";
    private static final String END = "2026-10-01T08:32:00+05:00";

    private final TripValidator validator = new TripValidator();

    @Test
    void acceptsTheExampleTripFromTheTask() {
        assertThatCode(() -> validator.validate(request("t1", START, END, 2400L, 360L))).doesNotThrowAnyException();
    }

    @Test
    void acceptsZeroCommissionAndCommissionEqualToAmount() {
        assertThatCode(() -> validator.validate(request("t1", START, END, 100L, 0L))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(request("t1", START, END, 100L, 100L))).doesNotThrowAnyException();
    }

    @Test
    void rejectsZeroAmount() {
        assertViolationOn(request("t1", START, END, 0L, 0L), "amount");
    }

    @Test
    void rejectsNegativeAmount() {
        assertViolationOn(request("t1", START, END, -5L, 0L), "amount");
    }

    @Test
    void rejectsEndEqualToStart() {
        assertViolationOn(request("t1", START, START, 100L, 10L), "end");
    }

    @Test
    void rejectsEndBeforeStart() {
        assertViolationOn(request("t1", START, "2026-10-01T08:00:00+05:00", 100L, 10L), "end");
    }

    @Test
    void comparesStartAndEndAsInstantsNotAsClockTimes() {
        // The end reads 09:00 (later than 08:10 on the clock) but 09:00+10:00 is 23:00Z the day before: earlier than the start.
        assertViolationOn(request("t1", START, "2026-10-01T09:00:00+10:00", 100L, 10L), "end");
    }

    @Test
    void rejectsCommissionAboveAmount() {
        assertViolationOn(request("t1", START, END, 100L, 101L), "commission");
    }

    @Test
    void rejectsNegativeCommission() {
        assertViolationOn(request("t1", START, END, 100L, -1L), "commission");
    }

    @Test
    void rejectsBlankAndTooLongIdentifier() {
        assertViolationOn(request("  ", START, END, 100L, 10L), "id");
        assertViolationOn(request("x".repeat(TripValidator.MAXIMUM_TRIP_IDENTIFIER_LENGTH + 1), START, END, 100L, 10L), "id");
    }

    @Test
    void reportsAllMissingFieldsAtOnce() {
        NewTripRequest emptyRequest = new NewTripRequest(null, null, null, null, null, null);

        assertThatThrownBy(() -> validator.validate(emptyRequest))
                .isInstanceOfSatisfying(InvalidTripException.class, exception ->
                        assertThat(exception.getViolations()).extracting(FieldViolation::fieldName)
                                .containsExactlyInAnyOrder("id", "start", "end", "amount", "payment", "commission"));
    }

    @Test
    void acceptsAmountAtTheLimitAndRejectsAboveIt() {
        assertThatCode(() -> validator.validate(request("t1", START, END, TripValidator.MAXIMUM_AMOUNT, 0L)))
                .doesNotThrowAnyException();
        assertViolationOn(request("t1", START, END, TripValidator.MAXIMUM_AMOUNT + 1, 0L), "amount");
    }

    @Test
    void acceptsTripOfExactlyMaximumDurationAndRejectsLongerOne() {
        assertThatCode(() -> validator.validate(request("t1", START, "2026-10-02T08:10:00+05:00", 100L, 10L)))
                .doesNotThrowAnyException();
        assertViolationOn(request("t1", START, "2026-10-02T08:10:01+05:00", 100L, 10L), "end");
    }

    private void assertViolationOn(NewTripRequest request, String expectedFieldName) {
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOfSatisfying(InvalidTripException.class, exception ->
                        assertThat(exception.getViolations()).extracting(FieldViolation::fieldName)
                                .contains(expectedFieldName));
    }

    private static NewTripRequest request(String tripIdentifier, String start, String end, long amount, long commission) {
        return new NewTripRequest(tripIdentifier, OffsetDateTime.parse(start), OffsetDateTime.parse(end),
                amount, PaymentMethod.CARD, commission);
    }
}
