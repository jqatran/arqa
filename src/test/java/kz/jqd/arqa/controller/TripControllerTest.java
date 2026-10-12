package kz.jqd.arqa.controller;

import kz.jqd.arqa.IntegrationTest;
import kz.jqd.arqa.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@AutoConfigureMockMvc
class TripControllerTest {

    private static final String CARD_TRIP = """
            {"id": "t1", "start": "2026-10-01T08:10:00+05:00", "end": "2026-10-01T08:32:00+05:00",
             "amount": 2400, "payment": "card", "commission": 360}""";

    private static final String CASH_TRIP = """
            {"id": "t2", "start": "2026-10-01T09:05:00+05:00", "end": "2026-10-01T09:20:00+05:00",
             "amount": 1500, "payment": "cash", "commission": 225}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TripRepository tripRepository;

    @BeforeEach
    void removeAllTrips() {
        tripRepository.deleteAll();
    }

    @Test
    void createsTripAndReturnsItWithTheOriginalOffset() throws Exception {
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON).content(CARD_TRIP))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("t1"))
                .andExpect(jsonPath("$.start").value("2026-10-01T08:10:00+05:00"))
                .andExpect(jsonPath("$.payment").value("card"))
                .andExpect(jsonPath("$.amount").value(2400));
    }

    @Test
    void repeatedPostReturnsOkAndDoesNotCreateADuplicate() throws Exception {
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON).content(CARD_TRIP))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON).content(CARD_TRIP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("t1"));

        mockMvc.perform(get("/api/trips").param("date", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        assertThat(tripRepository.count()).isEqualTo(1);
    }

    @Test
    void samePostWithChangedDataIsAConflict() throws Exception {
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON).content(CARD_TRIP))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON)
                        .content(CARD_TRIP.replace("2400", "2500")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_CONFLICT"));
    }

    @Test
    void rejectsNonPositiveAmountWithFieldDetails() throws Exception {
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON)
                        .content(CARD_TRIP.replace("2400", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations[?(@.fieldName == 'amount')]").exists());
    }

    @Test
    void rejectsEndNotLaterThanStart() throws Exception {
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON)
                        .content(CARD_TRIP.replace("08:32:00", "08:10:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[?(@.fieldName == 'end')]").exists());
    }

    @Test
    void rejectsUnknownPaymentMethod() throws Exception {
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON)
                        .content(CARD_TRIP.replace("\"card\"", "\"bitcoin\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void rejectsBrokenJson() throws Exception {
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsSummaryAndTripsOfTheRequestedDay() throws Exception {
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON).content(CARD_TRIP));
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON).content(CASH_TRIP));

        mockMvc.perform(get("/api/summary").param("date", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripCount").value(2))
                .andExpect(jsonPath("$.revenue").value(3900))
                .andExpect(jsonPath("$.commission").value(585))
                .andExpect(jsonPath("$.netEarnings").value(3315))
                .andExpect(jsonPath("$.cash.revenue").value(1500))
                .andExpect(jsonPath("$.card.revenue").value(2400));

        mockMvc.perform(get("/api/trips").param("date", "2026-10-01"))
                .andExpect(jsonPath("$[0].id").value("t1"))
                .andExpect(jsonPath("$[1].id").value("t2"));

        mockMvc.perform(get("/api/trips").param("date", "2026-10-02"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsMissingOrMalformedDate() throws Exception {
        mockMvc.perform(get("/api/summary")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/summary").param("date", "01.10.2026")).andExpect(status().isBadRequest());
    }

    @Test
    void idempotencyKeyHeaderIdentifiesTheTripWhenBodyHasNoId() throws Exception {
        String bodyWithoutIdentifier = CARD_TRIP.replace("\"id\": \"t1\", ", "");

        mockMvc.perform(post("/api/trips").header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON).content(bodyWithoutIdentifier))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("key-1"));
        mockMvc.perform(post("/api/trips").header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON).content(bodyWithoutIdentifier))
                .andExpect(status().isOk());
        assertThat(tripRepository.count()).isEqualTo(1);
    }

    @Test
    void idempotencyKeyDifferentFromBodyIdIsRejected() throws Exception {
        mockMvc.perform(post("/api/trips").header("Idempotency-Key", "other")
                        .contentType(MediaType.APPLICATION_JSON).content(CARD_TRIP))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].fieldName").value("Idempotency-Key"));
        assertThat(tripRepository.count()).isZero();
    }

    @Test
    void returnsTheEndWithItsOwnOffset() throws Exception {
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON)
                        .content(CARD_TRIP.replace("2026-10-01T08:32:00+05:00", "2026-10-01T05:32:00+02:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.end").value("2026-10-01T05:32:00+02:00"));
    }

    @Test
    void rejectsAmountAboveTheLimit() throws Exception {
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON)
                        .content(CARD_TRIP.replace("2400", "10000001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[?(@.fieldName == 'amount')]").exists());
    }

    @Test
    void summarizesAPeriodWithTotalsAndOnlyDaysThatHaveTrips() throws Exception {
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON).content(CARD_TRIP));
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON).content(CASH_TRIP));
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON)
                .content(CARD_TRIP.replace("t1", "t3").replace("2026-10-01", "2026-10-03")));

        mockMvc.perform(get("/api/summary/range").param("from", "2026-10-01").param("to", "2026-10-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-10-01"))
                .andExpect(jsonPath("$.to").value("2026-10-05"))
                .andExpect(jsonPath("$.tripCount").value(3))
                .andExpect(jsonPath("$.revenue").value(6300))
                .andExpect(jsonPath("$.netEarnings").value(5355))
                .andExpect(jsonPath("$.card.tripCount").value(2))
                .andExpect(jsonPath("$.days.length()").value(2))
                .andExpect(jsonPath("$.days[0].date").value("2026-10-01"))
                .andExpect(jsonPath("$.days[1].date").value("2026-10-03"));
    }

    @Test
    void pagesTripsOfARange() throws Exception {
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON).content(CARD_TRIP));
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON).content(CASH_TRIP));
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON)
                .content(CARD_TRIP.replace("t1", "t3").replace("2026-10-01", "2026-10-03")));

        mockMvc.perform(get("/api/trips/range").param("from", "2026-10-01").param("to", "2026-10-31")
                        .param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value("t1"))
                .andExpect(jsonPath("$.items[1].id").value("t2"))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/api/trips/range").param("from", "2026-10-01").param("to", "2026-10-31")
                        .param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value("t3"));
    }

    @Test
    void rejectsInvalidRangesAndPaging() throws Exception {
        mockMvc.perform(get("/api/summary/range").param("from", "2026-10-05").param("to", "2026-10-01"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_QUERY"));
        mockMvc.perform(get("/api/summary/range").param("from", "2026-01-01").param("to", "2026-12-31"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_QUERY"));
        mockMvc.perform(get("/api/trips/range").param("from", "2026-10-01").param("to", "2026-10-02").param("size", "0"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_QUERY"));
        mockMvc.perform(get("/api/trips/range").param("from", "2026-10-01").param("to", "2026-10-02").param("size", "201"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/trips/range").param("from", "2026-10-01").param("to", "2026-10-02").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/trips/range").param("from", "2026-10-01").param("to", "2026-10-02").param("page", "abc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        mockMvc.perform(get("/api/trips/range").param("from", "2026-10-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsFractionalAndStringNumbersInsteadOfSilentlyConvertingThem() throws Exception {
        for (String badAmount : new String[]{"1.5", "1e3", "\"2400\""}) {
            mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON)
                            .content(CARD_TRIP.replace("2400", badAmount)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        }
        assertThat(tripRepository.count()).isZero();
    }

    @Test
    void jsonNullBodyAndEmptyBodyAreBadRequestsNotServerErrors() throws Exception {
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON).content("null"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void errorsRaisedBySpringMvcUseTheSameFormatAndLanguage() throws Exception {
        mockMvc.perform(get("/api/nothing").param("lang", "ru"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Запрошенный ресурс не найден"));
        mockMvc.perform(delete("/api/trips"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        mockMvc.perform(post("/api/trips").contentType(MediaType.TEXT_PLAIN).content("{}").param("lang", "kk"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message").value(
                        "Мазмұн түріне қолдау жоқ: денені application/json ретінде жіберіңіз"));
    }
}
