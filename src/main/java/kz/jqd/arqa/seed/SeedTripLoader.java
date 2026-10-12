package kz.jqd.arqa.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.jqd.arqa.dto.NewTripRequest;
import kz.jqd.arqa.exception.InvalidTripException;
import kz.jqd.arqa.exception.TripConflictException;
import kz.jqd.arqa.service.interfaces.TripCreationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Loads the driver's trips from a JSON file on startup. Every trip goes through {@link TripCreationService#addTrip},
 * so the file gets the same validation and duplicate protection as the API, and restarts add nothing twice.
 * A broken record is logged and skipped; it never prevents the application from starting.
 */
@Component
@ConditionalOnProperty(name = "arqa.seed.enabled", havingValue = "true", matchIfMissing = true)
public class SeedTripLoader implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeedTripLoader.class);

    private final TripCreationService tripCreationService;
    private final ObjectMapper objectMapper;
    private final Resource seedResource;

    public SeedTripLoader(TripCreationService tripCreationService, ObjectMapper objectMapper,
                          @Value("${arqa.seed.resource}") Resource seedResource) {
        this.tripCreationService = tripCreationService;
        this.objectMapper = objectMapper;
        this.seedResource = seedResource;
    }

    @Override
    public void run(ApplicationArguments arguments) throws IOException {
        loadTrips();
    }

    /** @return number of trips that were newly stored by this call */
    public int loadTrips() throws IOException {
        List<NewTripRequest> requests;
        try (InputStream inputStream = seedResource.getInputStream()) {
            requests = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        }

        int newlyStoredTripCount = 0;
        for (NewTripRequest request : requests) {
            try {
                if (tripCreationService.addTrip(request).isNewlyCreated()) {
                    newlyStoredTripCount++;
                }
            } catch (InvalidTripException | TripConflictException exception) {
                LOGGER.warn("Skipping seed trip '{}': {}", request.tripIdentifier(), exception.getMessage());
            }
        }
        LOGGER.info("Seed file {}: {} new trips stored out of {}", seedResource.getFilename(),
                newlyStoredTripCount, requests.size());
        return newlyStoredTripCount;
    }
}
