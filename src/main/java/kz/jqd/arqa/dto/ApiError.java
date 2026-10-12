package kz.jqd.arqa.dto;

import java.util.List;

/**
 * Uniform error body. {@code code} is stable and never translated (clients switch on it);
 * {@code message} is in the language chosen by the client.
 */
public record ApiError(String code, String message, List<Violation> violations) {

    /** One broken rule: the JSON field, a stable message code and its translated text. */
    public record Violation(String fieldName, String code, String message) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }
}
