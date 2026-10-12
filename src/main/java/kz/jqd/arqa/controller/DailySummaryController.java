package kz.jqd.arqa.controller;

import kz.jqd.arqa.dto.DailySummaryResponse;
import kz.jqd.arqa.dto.PeriodSummaryResponse;
import kz.jqd.arqa.model.DateRange;
import kz.jqd.arqa.service.interfaces.DailySummaryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/summary")
public class DailySummaryController {

    private final DailySummaryService dailySummaryService;

    public DailySummaryController(DailySummaryService dailySummaryService) {
        this.dailySummaryService = dailySummaryService;
    }

    @GetMapping
    public DailySummaryResponse summarizeDay(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return DailySummaryResponse.from(dailySummaryService.summarizeDay(date));
    }

    /** Totals and per-day summaries for up to {@value DateRange#MAXIMUM_DAYS} days. */
    @GetMapping("/range")
    public PeriodSummaryResponse summarizePeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return PeriodSummaryResponse.from(dailySummaryService.summarizePeriod(new DateRange(from, to)));
    }
}
