package rs.teslaris.core.exception;

public class NonExistingRefreshTokenException extends RuntimeException {

    public NonExistingRefreshTokenException(String message) {
        super(message);
    }
}
