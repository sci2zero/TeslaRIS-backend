package rs.teslaris.core.util.exceptionhandling.exception;

import lombok.Getter;

@Getter
public class UnsupportedFilterException extends RuntimeException {

    private final String url;

    public UnsupportedFilterException(String message, String url) {
        super(message);
        this.url = url;
    }
}
