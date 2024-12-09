package rs.teslaris.core.util.exceptionhandling;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ErrorResponseUtil {

    public static ResponseEntity<Object> buildUnauthorisedResponse(HttpServletRequest request,
                                                                   String message) {
        return ResponseEntity.status(401)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(new ErrorObject(request, message,
                HttpStatus.UNAUTHORIZED));
    }

    public static ResponseEntity<Object> buildUnavailableResponse(HttpServletRequest request,
                                                                  String message) {
        return ResponseEntity.status(451)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(new ErrorObject(request, message,
                HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS));
    }
}
