package rs.teslaris.core.util.exceptionhandling;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

public class ErrorObject {

    public final String path;
    public final String message;
    public final LocalDateTime timestamp;
    public final Integer statusCode;
    public final String statusReason;

    public final Map<String, String> errors;

    public ErrorObject(HttpServletRequest request, String message, HttpStatus statusCode) {
        this.path = request.getRequestURI();
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.statusCode = statusCode.value();
        this.statusReason = statusCode.getReasonPhrase();
        this.errors = new HashMap<>();
    }

    public ErrorObject(HttpServletRequest request, String message, HttpStatus statusCode,
                       Map<String, String> errors) {
        this.path = request.getRequestURI();
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.statusCode = statusCode.value();
        this.statusReason = statusCode.getReasonPhrase();
        this.errors = errors;
    }

}
