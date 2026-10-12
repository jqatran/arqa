package kz.jqd.arqa.service;

import kz.jqd.arqa.IntegrationTest;
import kz.jqd.arqa.dto.NewTripRequest;
import kz.jqd.arqa.entity.Trip;
import kz.jqd.arqa.exception.InvalidTripException;
import kz.jqd.arqa.exception.TripConflictException;
import kz.jqd.arqa.model.AddTripResult;
import kz.jqd.arqa.model.PaymentMethod;
import kz.jqd.arqa.repository.TripRepository;
import kz.jqd.arqa.service.interfaces.TripCreationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class TripCreationServiceTest {

    @Autowired
    private TripCreationService tripCreationService;

    @Autowired
    private TripRepository tripRepository;

    @BeforeEach
    void removeAllTrips() {
        tripRepository.deleteAll();
    }

    @Test
    void storesAValidTrip() {
        AddTripResult result = tripCreationService.addTrip(validRequest("t1"));

        assertThat(result.isNewlyCreated()).isTrue();
        assertThat(tripRepository.findById("t1")).isPresent();
    }

    @Test
    void sendingTheSameTripTwiceDoesNotCreateADuplicate() {
        AddTripResult firstResult = tripCreationService.addTrip(validRequest("t1"));
        AddTripResult secondResult = tripCreationService.addTrip(validRequest("t1"));

        assertThat(firstResult.isNewlyCreated()).isTrue();
        assertThat(secondResult.isNewlyCreated()).isFalse();
        assertThat(tripRepository.count()).isEqualTo(1);
    }

    @Test
    void sameTripGivenInAnotherNotationOfTheSameInstantIsStillTheSameTrip() {
        tripCreationService.addTrip(validRequest("t1"));

        AddTripResult result = tripCreationService.addTrip(new NewTripRequest("t1",
                OffsetDateTime.parse("2026-10-01T08:10:00+05:00"),
                OffsetDateTime.parse("2026-10-01T03:32:00Z"),
                2400L, PaymentMethod.CARD, 360L));

        assertThat(result.isNewlyCreated()).isFalse();
        assertThat(tripRepository.count()).isEqualTo(1);
    }

    @Test
    void sameIdentifierWithDifferentDataIsAConflictAndKeepsTheOriginal() {
        tripCreationService.addTrip(validRequest("t1"));
        NewTripRequest changedRequest = new NewTripRequest("t1",
                OffsetDateTime.parse("2026-10-01T08:10:00+05:00"),
                OffsetDateTime.parse("2026-10-01T08:32:00+05:00"),
                9999L, PaymentMethod.CARD, 360L);

        assertThatThrownBy(() -> tripCreationService.addTrip(changedRequest)).isInstanceOf(TripConflictException.class);

        assertThat(tripRepository.count()).isEqualTo(1);
        assertThat(tripRepository.findById("t1")).get().extracting(Trip::getAmount).isEqualTo(2400L);
    }

    @RepeatedTest(30)
    void parallelIdenticalRequestsCreateExactlyOneTrip() throws Exception {
        int parallelRequestCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(parallelRequestCount);
        CountDownLatch startSignal = new CountDownLatch(1);
        try {
            List<Future<AddTripResult>> futures = new ArrayList<>();
            for (int requestNumber = 0; requestNumber < parallelRequestCount; requestNumber++) {
                Callable<AddTripResult> task = () -> {
                    startSignal.await();
                    return tripCreationService.addTrip(validRequest("race"));
                };
                futures.add(executor.submit(task));
            }
            startSignal.countDown();

            int newlyCreatedCount = 0;
            for (Future<AddTripResult> future : futures) {
                if (future.get().isNewlyCreated()) {
                    newlyCreatedCount++;
                }
            }

            assertThat(newlyCreatedCount).isEqualTo(1);
            assertThat(tripRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void invalidTripIsNotStored() {
        NewTripRequest endBeforeStart = new NewTripRequest("t1",
                OffsetDateTime.parse("2026-10-01T08:10:00+05:00"),
                OffsetDateTime.parse("2026-10-01T08:00:00+05:00"),
                100L, PaymentMethod.CASH, 10L);

        assertThatThrownBy(() -> tripCreationService.addTrip(endBeforeStart)).isInstanceOf(InvalidTripException.class);

        assertThat(tripRepository.count()).isZero();
    }

    private static NewTripRequest validRequest(String tripIdentifier) {
        return new NewTripRequest(tripIdentifier,
                OffsetDateTime.parse("2026-10-01T08:10:00+05:00"),
                OffsetDateTime.parse("2026-10-01T08:32:00+05:00"),
                2400L, PaymentMethod.CARD, 360L);
    }
}
