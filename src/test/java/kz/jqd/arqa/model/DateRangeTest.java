package kz.jqd.arqa.model;

import kz.jqd.arqa.exception.InvalidQueryException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {

    private static final LocalDate FIRST_DAY = LocalDate.of(2026, 10, 1);

    @Test
    void acceptsASingleDay() {
        assertThat(new DateRange(FIRST_DAY, FIRST_DAY).from()).isEqualTo(FIRST_DAY);
    }

    @Test
    void acceptsTheMaximumNumberOfDays() {
        LocalDate lastAllowedDay = FIRST_DAY.plusDays(DateRange.MAXIMUM_DAYS - 1);

        assertThat(new DateRange(FIRST_DAY, lastAllowedDay).to()).isEqualTo(lastAllowedDay);
    }

    @Test
    void rejectsOneDayMoreThanTheMaximum() {
        assertThatThrownBy(() -> new DateRange(FIRST_DAY, FIRST_DAY.plusDays(DateRange.MAXIMUM_DAYS)))
                .isInstanceOf(InvalidQueryException.class);
    }

    @Test
    void rejectsInvertedRange() {
        assertThatThrownBy(() -> new DateRange(FIRST_DAY, FIRST_DAY.minusDays(1)))
                .isInstanceOf(InvalidQueryException.class);
    }

    @Test
    void rejectsMissingBounds() {
        assertThatThrownBy(() -> new DateRange(null, FIRST_DAY)).isInstanceOf(InvalidQueryException.class);
        assertThatThrownBy(() -> new DateRange(FIRST_DAY, null)).isInstanceOf(InvalidQueryException.class);
    }
}
