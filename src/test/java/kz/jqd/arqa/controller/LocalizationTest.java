package kz.jqd.arqa.controller;

import kz.jqd.arqa.IntegrationTest;
import kz.jqd.arqa.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@AutoConfigureMockMvc
class LocalizationTest {

    private static final String ZERO_AMOUNT_TRIP = """
            {"id": "t1", "start": "2026-10-01T08:10:00+05:00", "end": "2026-10-01T08:32:00+05:00",
             "amount": 0, "payment": "card", "commission": 0}""";

    private static final String VALID_TRIP = ZERO_AMOUNT_TRIP.replace("\"amount\": 0", "\"amount\": 2400");

    private static final String TOO_LONG_TRIP = ZERO_AMOUNT_TRIP
            .replace("\"amount\": 0", "\"amount\": 5")
            .replace("2026-10-01T08:32:00", "2026-10-03T08:32:00");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TripRepository tripRepository;

    @BeforeEach
    void removeAllTrips() {
        tripRepository.deleteAll();
    }

    private MockHttpServletRequestBuilder postTrip(String body) {
        return post("/api/trips").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void validationMessagesAreEnglishByDefault() throws Exception {
        mockMvc.perform(postTrip(ZERO_AMOUNT_TRIP))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "en"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Trip data is invalid"))
                .andExpect(jsonPath("$.violations[0].fieldName").value("amount"))
                .andExpect(jsonPath("$.violations[0].code").value("trip.amount.notPositive"))
                .andExpect(jsonPath("$.violations[0].message").value("must be greater than 0"));
    }

    @Test
    void validationMessagesInRussian() throws Exception {
        mockMvc.perform(postTrip(ZERO_AMOUNT_TRIP).header(HttpHeaders.ACCEPT_LANGUAGE, "ru-RU,ru;q=0.9"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "ru"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Данные поездки некорректны"))
                .andExpect(jsonPath("$.violations[0].message").value("должна быть больше 0"));
    }

    @Test
    void validationMessagesInKazakh() throws Exception {
        mockMvc.perform(postTrip(ZERO_AMOUNT_TRIP).header(HttpHeaders.ACCEPT_LANGUAGE, "kk-KZ"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "kk"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Сапар деректері қате"))
                .andExpect(jsonPath("$.violations[0].message").value("0-ден үлкен болуы керек"));
    }

    @Test
    void languageQueryParameterWinsOverTheHeader() throws Exception {
        mockMvc.perform(postTrip(ZERO_AMOUNT_TRIP).param("lang", "kk").header(HttpHeaders.ACCEPT_LANGUAGE, "ru"))
                .andExpect(jsonPath("$.message").value("Сапар деректері қате"));
    }

    @Test
    void unsupportedLanguageFallsBackToEnglish() throws Exception {
        mockMvc.perform(postTrip(ZERO_AMOUNT_TRIP).header(HttpHeaders.ACCEPT_LANGUAGE, "fr-FR"))
                .andExpect(jsonPath("$.message").value("Trip data is invalid"));
        mockMvc.perform(postTrip(ZERO_AMOUNT_TRIP).param("lang", "zz"))
                .andExpect(jsonPath("$.message").value("Trip data is invalid"));
    }

    @Test
    void messageArgumentsAreFilledInEveryLanguage() throws Exception {
        mockMvc.perform(postTrip(TOO_LONG_TRIP))
                .andExpect(jsonPath("$.violations[0].message").value("trip must not last longer than 24 hours"));
        mockMvc.perform(postTrip(TOO_LONG_TRIP).header(HttpHeaders.ACCEPT_LANGUAGE, "ru"))
                .andExpect(jsonPath("$.violations[0].message").value("поездка не должна длиться дольше 24 ч"));
        mockMvc.perform(postTrip(TOO_LONG_TRIP).header(HttpHeaders.ACCEPT_LANGUAGE, "kk"))
                .andExpect(jsonPath("$.violations[0].message").value("сапар 24 сағаттан ұзақ болмауы керек"));
    }

    @Test
    void conflictMessageNamesTheTripInEveryLanguage() throws Exception {
        mockMvc.perform(postTrip(VALID_TRIP)).andExpect(status().isCreated());
        String changed = VALID_TRIP.replace("2400", "2500");

        mockMvc.perform(postTrip(changed))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Trip \"t1\" already exists with different data"));
        mockMvc.perform(postTrip(changed).param("lang", "ru"))
                .andExpect(jsonPath("$.message").value("Поездка «t1» уже существует с другими данными"));
        mockMvc.perform(postTrip(changed).param("lang", "kk"))
                .andExpect(jsonPath("$.message").value("«t1» сапары басқа деректермен бұрыннан бар"));
    }

    @Test
    void queryAndParameterErrorsAreTranslated() throws Exception {
        mockMvc.perform(get("/api/summary/range").param("from", "2026-10-05").param("to", "2026-10-01").param("lang", "ru"))
                .andExpect(jsonPath("$.code").value("INVALID_QUERY"))
                .andExpect(jsonPath("$.message").value("\"to\" не может быть раньше \"from\""));
        mockMvc.perform(get("/api/summary/range").param("from", "2026-01-01").param("to", "2026-12-31").param("lang", "kk"))
                .andExpect(jsonPath("$.message").value("Кезең 92 күннен аспауы керек"));
        mockMvc.perform(get("/api/summary").param("date", "oops").param("lang", "ru"))
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value(
                        "Параметр запроса отсутствует или имеет неверный формат (даты — YYYY-MM-DD, page и size — целые числа)"));
        mockMvc.perform(postTrip("{not json").param("lang", "kk"))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void idempotencyKeyMismatchIsTranslated() throws Exception {
        mockMvc.perform(postTrip(VALID_TRIP).header("Idempotency-Key", "other").param("lang", "ru"))
                .andExpect(jsonPath("$.violations[0].fieldName").value("Idempotency-Key"))
                .andExpect(jsonPath("$.violations[0].message")
                        .value("должен совпадать с \"id\" поездки, если переданы оба значения"));
    }
}
