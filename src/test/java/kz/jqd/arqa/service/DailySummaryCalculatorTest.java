package kz.jqd.arqa.service;

import kz.jqd.arqa.entity.Trip;
import kz.jqd.arqa.model.DailySummary;
import kz.jqd.arqa.model.PaymentMethod;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DailySummaryCalculatorTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 1);

    private final DailySummaryCalculator calculator = new DailySummaryCalculator();

    @Test
    void summarizesTheExampleDayFromTheTask() {
        List<Trip> trips = List.of(
                trip("t1", 2400, PaymentMethod.CARD, 360),
                trip("t2", 1500, PaymentMethod.CASH, 225));

        DailySummary summary = calculator.calculate(DAY, trips);

        assertThat(summary.date()).isEqualTo(DAY);
        assertThat(summary.tripCount()).isEqualTo(2);
        assertThat(summary.revenue()).isEqualTo(3900);
        assertThat(summary.commission()).isEqualTo(585);
        assertThat(summary.netEarnings()).isEqualTo(3315);
        assertThat(summary.card()).isEqualTo(new DailySummary.PaymentBreakdown(1, 2400, 360));
        assertThat(summary.cash()).isEqualTo(new DailySummary.PaymentBreakdown(1, 1500, 225));
    }

    @Test
    void returnsZerosForADayWithoutTrips() {
        DailySummary summary = calculator.calculate(DAY, List.of());

        assertThat(summary.tripCount()).isZero();
        assertThat(summary.revenue()).isZero();
        assertThat(summary.commission()).isZero();
        assertThat(summary.netEarnings()).isZero();
        assertThat(summary.cash()).isEqualTo(new DailySummary.PaymentBreakdown(0, 0, 0));
        assertThat(summary.card()).isEqualTo(new DailySummary.PaymentBreakdown(0, 0, 0));
    }

    @Test
    void countsOnlyCashWhenAllTripsArePaidInCash() {
        List<Trip> trips = List.of(
                trip("t1", 1000, PaymentMethod.CASH, 100),
                trip("t2", 2000, PaymentMethod.CASH, 300),
                trip("t3", 500, PaymentMethod.CASH, 0));

        DailySummary summary = calculator.calculate(DAY, trips);

        assertThat(summary.cash()).isEqualTo(new DailySummary.PaymentBreakdown(3, 3500, 400));
        assertThat(summary.card()).isEqualTo(new DailySummary.PaymentBreakdown(0, 0, 0));
        assertThat(summary.netEarnings()).isEqualTo(3100);
    }

    @Test
    void breakdownAddsUpToTheTotals() {
        List<Trip> trips = List.of(
                trip("t1", 1234, PaymentMethod.CARD, 185),
                trip("t2", 4321, PaymentMethod.CASH, 648),
                trip("t3", 999, PaymentMethod.CARD, 149),
                trip("t4", 10, PaymentMethod.CASH, 1));

        DailySummary summary = calculator.calculate(DAY, trips);

        assertThat(summary.cash().revenue() + summary.card().revenue()).isEqualTo(summary.revenue());
        assertThat(summary.cash().commission() + summary.card().commission()).isEqualTo(summary.commission());
        assertThat(summary.cash().tripCount() + summary.card().tripCount()).isEqualTo(summary.tripCount());
        assertThat(summary.netEarnings()).isEqualTo(summary.revenue() - summary.commission());
    }

    private static Trip trip(String tripIdentifier, long amount, PaymentMethod paymentMethod, long commission) {
        return new Trip(tripIdentifier,
                OffsetDateTime.parse("2026-10-01T08:00:00+05:00"),
                OffsetDateTime.parse("2026-10-01T08:30:00+05:00"),
                amount, paymentMethod, commission);
    }
}
