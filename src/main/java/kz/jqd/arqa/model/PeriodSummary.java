package kz.jqd.arqa.model;

import java.util.List;

/** Totals for a range of days plus the summary of each day that has trips (days without trips are omitted). */
public record PeriodSummary(DateRange range, DailySummary total, List<DailySummary> days) {
}
