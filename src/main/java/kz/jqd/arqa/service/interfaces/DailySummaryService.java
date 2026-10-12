package kz.jqd.arqa.service.interfaces;

import kz.jqd.arqa.model.DailySummary;
import kz.jqd.arqa.model.DateRange;
import kz.jqd.arqa.model.PeriodSummary;

import java.time.LocalDate;

public interface DailySummaryService {

    DailySummary summarizeDay(LocalDate date);

    /** Totals and per-day summaries for the range; days without trips are not listed. */
    PeriodSummary summarizePeriod(DateRange range);
}
