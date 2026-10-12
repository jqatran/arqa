package kz.jqd.arqa.exception;

/** A trip with the same identifier already exists but with different data. */
public class TripConflictException extends ArqaException {

    public TripConflictException(String tripIdentifier) {
        super("error.tripConflict", tripIdentifier);
    }
}
