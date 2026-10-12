package kz.jqd.arqa.exception;

import kz.jqd.arqa.model.FieldViolation;

import java.util.List;

public class InvalidTripException extends ArqaException {

    private final List<FieldViolation> violations;

    public InvalidTripException(List<FieldViolation> violations) {
        super("error.validationFailed");
        this.violations = List.copyOf(violations);
    }

    public List<FieldViolation> getViolations() {
        return violations;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " " + violations;
    }
}
