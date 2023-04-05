package rs.teslaris.core.exception;

public class WrongPasswordProvidedException extends RuntimeException {

    public WrongPasswordProvidedException(String message) {
        super(message);
    }
}
