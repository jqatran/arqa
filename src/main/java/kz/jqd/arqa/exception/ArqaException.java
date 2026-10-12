package kz.jqd.arqa.exception;

import java.util.Arrays;
import java.util.List;

/**
 * Base of the application's business exceptions. It carries a message code instead of finished text:
 * the language is a concern of the web layer, which translates the code for each request.
 * The unchecked type is deliberate: these are rule violations handled in one place ({@code ApiExceptionHandler}),
 * and checked exceptions would only add {@code throws} clauses to every layer in between.
 */
public abstract class ArqaException extends RuntimeException {

    private final String messageCode;
    private final List<Object> messageArguments;

    protected ArqaException(String messageCode, Object... messageArguments) {
        // The diagnostic text for logs is the code with its arguments; clients get the translated text.
        super(messageCode + Arrays.toString(messageArguments));
        this.messageCode = messageCode;
        this.messageArguments = List.of(messageArguments);
    }

    public String getMessageCode() {
        return messageCode;
    }

    public List<Object> getMessageArguments() {
        return messageArguments;
    }
}
