package kz.jqd.arqa.dto;

import kz.jqd.arqa.model.DailySummary;

import java.time.LocalDate;

/** Daily summary as returned by the API; decouples the public contract from the domain model. */
public record DailySummaryResponse(
        LocalDate date,
        int tripCount,
        long revenue,
        long commission,
        long netEarnings,
        PaymentBreakdownResponse cash,
        PaymentBreakdownResponse card) {

    public record PaymentBreakdownResponse(int tripCount, long revenue, long commission) {

        public static PaymentBreakdownResponse from(DailySummary.PaymentBreakdown breakdown) {
            return new PaymentBreakdownResponse(breakdown.tripCount(), breakdown.revenue(), breakdown.commission());
        }
    }

    public static DailySummaryResponse from(DailySummary summary) {
        return new DailySummaryResponse(summary.date(), summary.tripCount(), summary.revenue(),
                summary.commission(), summary.netEarnings(),
                PaymentBreakdownResponse.from(summary.cash()), PaymentBreakdownResponse.from(summary.card()));
    }
}
