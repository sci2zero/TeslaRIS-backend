package rs.teslaris.core.util.exceptionhandling.exception;

import lombok.Getter;

@Getter
public class UnsupportedEntityTypeException extends RuntimeException {

    private final String url;

    public UnsupportedEntityTypeException(String message, String url) {
        super(message);
        this.url = url;
    }
}
