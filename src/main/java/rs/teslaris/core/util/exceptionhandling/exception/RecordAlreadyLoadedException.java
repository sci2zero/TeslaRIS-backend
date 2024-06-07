package rs.teslaris.core.util.exceptionhandling.exception;

public class RecordAlreadyLoadedException extends RuntimeException {

    public RecordAlreadyLoadedException(String message) {
        super(message);
    }
}
