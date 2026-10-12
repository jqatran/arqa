package kz.jqd.arqa.exception;

/** A read request has parameters that make no sense, e.g. an inverted or too long date range. */
public class InvalidQueryException extends ArqaException {

    public InvalidQueryException(String messageCode, Object... messageArguments) {
        super(messageCode, messageArguments);
    }
}
