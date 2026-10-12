package kz.jqd.arqa.entity;

import kz.jqd.arqa.model.PaymentMethod;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class TripTest {

    private TimeZone originalTimeZone;

    @BeforeEach
    void switchServerToUtc() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void restoreServerTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    void tripJustAfterLocalMidnightBelongsToTheLocalDayEvenIfItIsThePreviousDayInUtc() {
        Trip trip = new Trip("t1",
                OffsetDateTime.parse("2026-10-02T00:10:00+05:00"),
                OffsetDateTime.parse("2026-10-02T00:30:00+05:00"),
                1000, PaymentMethod.CASH, 150);

        assertThat(trip.getLocalTripDate()).isEqualTo(LocalDate.of(2026, 10, 2));
    }

    @Test
    void tripCrossingMidnightBelongsToTheDayItStartedOn() {
        Trip trip = new Trip("t1",
                OffsetDateTime.parse("2026-10-02T23:50:00+05:00"),
                OffsetDateTime.parse("2026-10-03T00:15:00+05:00"),
                1000, PaymentMethod.CASH, 150);

        assertThat(trip.getLocalTripDate()).isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(trip.getStartedAt().toString()).isEqualTo("2026-10-02T23:50+05:00");
        assertThat(trip.getEndedAt().toString()).isEqualTo("2026-10-03T00:15+05:00");
    }

    @Test
    void keepsTheOffsetOfTheEndSeparatelyFromTheStart() {
        Trip trip = new Trip("t1",
                OffsetDateTime.parse("2026-10-02T23:50:00+05:00"),
                OffsetDateTime.parse("2026-10-02T20:15:00+02:00"),
                1000, PaymentMethod.CASH, 150);

        assertThat(trip.getStartedAt().toString()).isEqualTo("2026-10-02T23:50+05:00");
        assertThat(trip.getEndedAt().toString()).isEqualTo("2026-10-02T20:15+02:00");
    }

    @Test
    void sameMomentWrittenWithAnotherEndOffsetIsTheSameTrip() {
        Trip original = new Trip("t1", OffsetDateTime.parse("2026-10-02T10:00:00+05:00"),
                OffsetDateTime.parse("2026-10-02T10:30:00+05:00"), 1000, PaymentMethod.CASH, 150);
        Trip rewritten = new Trip("t1", OffsetDateTime.parse("2026-10-02T10:00:00+05:00"),
                OffsetDateTime.parse("2026-10-02T05:30:00Z"), 1000, PaymentMethod.CASH, 150);
        Trip differentStartOffset = new Trip("t1", OffsetDateTime.parse("2026-10-02T05:00:00Z"),
                OffsetDateTime.parse("2026-10-02T05:30:00Z"), 1000, PaymentMethod.CASH, 150);

        assertThat(original.hasSameDataAs(rewritten)).isTrue();
        assertThat(original.hasSameDataAs(differentStartOffset)).isFalse();
    }
}
