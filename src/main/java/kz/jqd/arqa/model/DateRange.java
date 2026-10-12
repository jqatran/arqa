package kz.jqd.arqa.model;

import kz.jqd.arqa.exception.InvalidQueryException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Inclusive range of driver-local days. Bounded so a single request cannot load the whole history. */
public record DateRange(LocalDate from, LocalDate to) {

    public static final int MAXIMUM_DAYS = 92;

    public DateRange {
        if (from == null || to == null) {
            throw new InvalidQueryException("query.range.bounds.required");
        }
        if (to.isBefore(from)) {
            throw new InvalidQueryException("query.range.inverted");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAXIMUM_DAYS) {
            throw new InvalidQueryException("query.range.tooLong", MAXIMUM_DAYS);
        }
    }
}
