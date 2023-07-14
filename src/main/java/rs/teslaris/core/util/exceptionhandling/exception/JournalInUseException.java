package rs.teslaris.core.util.exceptionhandling.exception;

public class JournalInUseException extends RuntimeException {

    public JournalInUseException(String message) {
        super(message);
    }
}
