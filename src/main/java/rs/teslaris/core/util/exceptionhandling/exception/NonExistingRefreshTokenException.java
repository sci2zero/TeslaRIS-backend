package rs.teslaris.core.util.exceptionhandling.exception;

public class NonExistingRefreshTokenException extends RuntimeException {

    public NonExistingRefreshTokenException(String message) {
        super(message);
    }
}
