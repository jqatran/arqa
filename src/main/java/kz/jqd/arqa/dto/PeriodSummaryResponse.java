package kz.jqd.arqa.dto;

import kz.jqd.arqa.dto.DailySummaryResponse.PaymentBreakdownResponse;
import kz.jqd.arqa.model.PeriodSummary;

import java.time.LocalDate;
import java.util.List;

/** Totals for a range of days and the summary of every day that has trips. */
public record PeriodSummaryResponse(
        LocalDate from,
        LocalDate to,
        int tripCount,
        long revenue,
        long commission,
        long netEarnings,
        PaymentBreakdownResponse cash,
        PaymentBreakdownResponse card,
        List<DailySummaryResponse> days) {

    public static PeriodSummaryResponse from(PeriodSummary summary) {
        return new PeriodSummaryResponse(
                summary.range().from(), summary.range().to(),
                summary.total().tripCount(), summary.total().revenue(), summary.total().commission(),
                summary.total().netEarnings(),
                PaymentBreakdownResponse.from(summary.total().cash()),
                PaymentBreakdownResponse.from(summary.total().card()),
                summary.days().stream().map(DailySummaryResponse::from).toList());
    }
}
