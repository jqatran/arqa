package kz.jqd.arqa.service;

import kz.jqd.arqa.entity.Trip;
import kz.jqd.arqa.model.DailySummary;
import kz.jqd.arqa.model.PaymentMethod;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DailySummaryCalculator {

    public DailySummary calculate(LocalDate date, List<Trip> trips) {
        DailySummary.PaymentBreakdown cash = breakdownFor(trips, PaymentMethod.CASH);
        DailySummary.PaymentBreakdown card = breakdownFor(trips, PaymentMethod.CARD);

        long revenue = cash.revenue() + card.revenue();
        long commission = cash.commission() + card.commission();
        return new DailySummary(date, trips.size(), revenue, commission, revenue - commission, cash, card);
    }

    private DailySummary.PaymentBreakdown breakdownFor(List<Trip> trips, PaymentMethod paymentMethod) {
        List<Trip> matchingTrips = trips.stream()
                .filter(trip -> trip.getPaymentMethod() == paymentMethod)
                .toList();
        long revenue = matchingTrips.stream().mapToLong(Trip::getAmount).sum();
        long commission = matchingTrips.stream().mapToLong(Trip::getCommission).sum();
        return new DailySummary.PaymentBreakdown(matchingTrips.size(), revenue, commission);
    }
}
