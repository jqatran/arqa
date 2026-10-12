package kz.jqd.arqa.dto;

import kz.jqd.arqa.exception.InvalidTripException;
import kz.jqd.arqa.model.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewTripRequestTest {

    private static NewTripRequest requestWithIdentifier(String tripIdentifier) {
        return new NewTripRequest(tripIdentifier, OffsetDateTime.parse("2026-10-01T08:10:00+05:00"),
                OffsetDateTime.parse("2026-10-01T08:32:00+05:00"), 2400L, PaymentMethod.CARD, 360L);
    }

    @Test
    void headerNamesTheTripWhenBodyHasNoIdentifier() {
        assertThat(requestWithIdentifier(null).withIdempotencyKey("key-1").tripIdentifier()).isEqualTo("key-1");
        assertThat(requestWithIdentifier("  ").withIdempotencyKey("key-1").tripIdentifier()).isEqualTo("key-1");
    }

    @Test
    void sameHeaderAndBodyIdentifierIsAccepted() {
        NewTripRequest request = requestWithIdentifier("key-1");

        assertThat(request.withIdempotencyKey("key-1")).isSameAs(request);
    }

    @Test
    void differentHeaderAndBodyIdentifierIsRejected() {
        assertThatThrownBy(() -> requestWithIdentifier("t1").withIdempotencyKey("t2"))
                .isInstanceOfSatisfying(InvalidTripException.class, exception ->
                        assertThat(exception.getViolations().get(0).fieldName()).isEqualTo("Idempotency-Key"));
    }

    @Test
    void missingOrBlankHeaderChangesNothing() {
        NewTripRequest request = requestWithIdentifier("t1");

        assertThat(request.withIdempotencyKey(null)).isSameAs(request);
        assertThat(request.withIdempotencyKey("   ")).isSameAs(request);
    }
}
