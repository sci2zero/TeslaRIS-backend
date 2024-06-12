package rs.teslaris.core.util.exceptionhandling.exception;

public class CantConstructRestTemplateException extends RuntimeException {

    public CantConstructRestTemplateException(String message) {
        super(message);
    }
}
