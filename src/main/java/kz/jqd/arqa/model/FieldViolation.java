package kz.jqd.arqa.model;

import java.util.List;

/**
 * A single validation problem. {@code fieldName} is the JSON name of the offending field;
 * {@code messageCode} and {@code messageArguments} identify the text, which the web layer translates
 * into the language chosen by the client.
 */
public record FieldViolation(String fieldName, String messageCode, List<Object> messageArguments) {

    public FieldViolation(String fieldName, String messageCode) {
        this(fieldName, messageCode, List.of());
    }
}
