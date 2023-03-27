package rs.teslaris.core.util.exceptionhandling;

import java.time.LocalDateTime;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

public class ErrorObject {

    public final String path;
    public final String message;
    public final LocalDateTime timestamp;
    public final Integer statusCode;
    public final String statusReason;


    public ErrorObject(HttpServletRequest request, String message, HttpStatus statusCode) {
        this.path = request.getRequestURI();
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.statusCode = statusCode.value();
        this.statusReason = statusCode.getReasonPhrase();
    }

}
