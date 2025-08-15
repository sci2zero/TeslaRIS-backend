package rs.teslaris.core.util.exceptionhandling.exception;

public class InvalidOAuth2CodeException extends RuntimeException {

    public InvalidOAuth2CodeException(String message) {
        super(message);
    }
}
