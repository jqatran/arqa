package kz.jqd.arqa.controller;

import kz.jqd.arqa.dto.ApiError;
import kz.jqd.arqa.exception.ArqaException;
import kz.jqd.arqa.exception.InvalidQueryException;
import kz.jqd.arqa.exception.InvalidTripException;
import kz.jqd.arqa.exception.TripConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Locale;

/**
 * Turns every exception into an {@link ApiError} body, translating messages into the request's language.
 * Extending {@link ResponseEntityExceptionHandler} routes the errors Spring MVC raises itself (404, 405, 415, ...)
 * through the same format, so clients see one error shape for everything.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final MessageSource messageSource;

    public ApiExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(InvalidTripException.class)
    public ResponseEntity<ApiError> handleInvalidTrip(InvalidTripException exception, Locale locale) {
        List<ApiError.Violation> violations = exception.getViolations().stream()
                .map(violation -> new ApiError.Violation(violation.fieldName(), violation.messageCode(),
                        translate(violation.messageCode(), violation.messageArguments(), locale)))
                .toList();
        ApiError body = new ApiError("VALIDATION_FAILED", translate(exception, locale), violations);
        return respond(HttpStatus.BAD_REQUEST, new HttpHeaders(), body, locale);
    }

    @ExceptionHandler(TripConflictException.class)
    public ResponseEntity<ApiError> handleTripConflict(TripConflictException exception, Locale locale) {
        ApiError body = ApiError.of("TRIP_CONFLICT", translate(exception, locale));
        return respond(HttpStatus.CONFLICT, new HttpHeaders(), body, locale);
    }

    @ExceptionHandler(InvalidQueryException.class)
    public ResponseEntity<ApiError> handleInvalidQuery(InvalidQueryException exception, Locale locale) {
        ApiError body = ApiError.of("INVALID_QUERY", translate(exception, locale));
        return respond(HttpStatus.BAD_REQUEST, new HttpHeaders(), body, locale);
    }

    /** Anything not handled above is a bug on our side: log it fully, tell the client only that it failed. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, Locale locale) {
        LOGGER.error("Unexpected error while handling a request", exception);
        ApiError body = ApiError.of("INTERNAL_ERROR", translate("error.internal", List.of(), locale));
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, new HttpHeaders(), body, locale);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respondObject(status, headers, "MALFORMED_REQUEST", "error.malformedRequest");
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respondObject(status, headers, "INVALID_PARAMETER", "error.invalidParameter");
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respondObject(status, headers, "INVALID_PARAMETER", "error.invalidParameter");
    }

    /** Fallback for every other Spring MVC exception (404, 405, 415, 406, ...). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, @Nullable Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respondObject(status, headers, codeFor(status), messageCodeFor(status));
    }

    private ResponseEntity<Object> respondObject(HttpStatusCode status, HttpHeaders headers, String code, String messageCode) {
        Locale locale = LocaleContextHolder.getLocale();
        ApiError body = ApiError.of(code, translate(messageCode, List.of(), locale));
        return ResponseEntity.status(status).headers(withLanguage(headers, locale)).body(body);
    }

    private static String codeFor(HttpStatusCode status) {
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        return knownStatus == null ? "ERROR" : knownStatus.name();
    }

    private static String messageCodeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "error.notFound";
            case 405 -> "error.methodNotAllowed";
            case 406 -> "error.notAcceptable";
            case 415 -> "error.unsupportedMediaType";
            default -> status.is5xxServerError() ? "error.internal" : "error.badRequest";
        };
    }

    private String translate(ArqaException exception, Locale locale) {
        return translate(exception.getMessageCode(), exception.getMessageArguments(), locale);
    }

    private String translate(String messageCode, List<Object> messageArguments, Locale locale) {
        return messageSource.getMessage(messageCode, messageArguments.toArray(), locale);
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, HttpHeaders headers, ApiError body, Locale locale) {
        return ResponseEntity.status(status).headers(withLanguage(headers, locale)).body(body);
    }

    /** Keeps headers Spring already chose (for example {@code Allow} on a 405) and states the response language. */
    private static HttpHeaders withLanguage(HttpHeaders headers, Locale locale) {
        HttpHeaders result = new HttpHeaders();
        result.putAll(headers);
        result.setContentLanguage(locale);
        return result;
    }
}
