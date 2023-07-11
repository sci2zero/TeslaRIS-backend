package rs.teslaris.core.util.exceptionhandling.exception;

public class WrongPasswordProvidedException extends RuntimeException {

    public WrongPasswordProvidedException(String message) {
        super(message);
    }
}
