package kz.jqd.arqa.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.jqd.arqa.IntegrationTest;
import kz.jqd.arqa.repository.TripRepository;
import kz.jqd.arqa.service.interfaces.DailySummaryService;
import kz.jqd.arqa.service.interfaces.TripCreationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class SeedTripLoaderTest {

    @Autowired
    private TripCreationService tripCreationService;

    @Autowired
    private DailySummaryService dailySummaryService;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void removeAllTrips() {
        tripRepository.deleteAll();
    }

    @Test
    void loadingTheShippedFileTwiceStoresEachTripOnce() throws Exception {
        SeedTripLoader loader = new SeedTripLoader(tripCreationService, objectMapper, new ClassPathResource("seed/trips.json"));

        int firstLoadCount = loader.loadTrips();
        int secondLoadCount = loader.loadTrips();

        assertThat(firstLoadCount).isEqualTo(8);
        assertThat(secondLoadCount).isZero();
        assertThat(tripRepository.count()).isEqualTo(8);
        assertThat(dailySummaryService.summarizeDay(LocalDate.of(2026, 10, 1)).netEarnings()).isEqualTo(3315 + 2720);
    }

    @Test
    void invalidRecordsAreSkippedAndValidOnesAreStored() throws Exception {
        String json = """
                [
                  {"id": "good", "start": "2026-10-01T08:10:00+05:00", "end": "2026-10-01T08:32:00+05:00",
                   "amount": 2400, "payment": "card", "commission": 360},
                  {"id": "bad", "start": "2026-10-01T09:10:00+05:00", "end": "2026-10-01T09:00:00+05:00",
                   "amount": 100, "payment": "cash", "commission": 10}
                ]""";
        SeedTripLoader loader = new SeedTripLoader(tripCreationService, objectMapper, new ByteArrayResource(json.getBytes()));

        assertThat(loader.loadTrips()).isEqualTo(1);
        assertThat(tripRepository.findById("bad")).isEmpty();
    }
}
