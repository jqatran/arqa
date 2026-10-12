package kz.jqd.arqa.model;

import java.time.LocalDate;

/**
 * Summary of one driver's day. {@code netEarnings} is revenue minus commission ("на руки").
 */
public record DailySummary(
        LocalDate date,
        int tripCount,
        long revenue,
        long commission,
        long netEarnings,
        PaymentBreakdown cash,
        PaymentBreakdown card) {

    public record PaymentBreakdown(int tripCount, long revenue, long commission) {
    }
}
