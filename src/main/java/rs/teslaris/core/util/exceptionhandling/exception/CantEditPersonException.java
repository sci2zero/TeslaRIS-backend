package rs.teslaris.core.util.exceptionhandling.exception;

public class CantEditPersonException extends RuntimeException {

    public CantEditPersonException(String message) {
        super(message);
    }
}
