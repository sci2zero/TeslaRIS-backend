package rs.teslaris.core.util.exceptionhandling;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Component
public class ErrorResponseUtil {

    private static ObjectMapper objectMapper;


    @Autowired
    private ErrorResponseUtil(ObjectMapper objectMapper) {
        ErrorResponseUtil.objectMapper = objectMapper;
    }

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

    public static ResponseEntity<StreamingResponseBody> buildUnauthorisedStreamingResponse(
        HttpServletRequest request,
        String message) {

        return ResponseEntity.status(401)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(outputStream -> {
                String jsonResponse = objectMapper.writeValueAsString(
                    new ErrorObject(request, message, HttpStatus.UNAUTHORIZED));
                outputStream.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
            });
    }

    public static ResponseEntity<StreamingResponseBody> buildUnavailableStreamingResponse(
        HttpServletRequest request,
        String message) {

        return ResponseEntity.status(451)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(outputStream -> {
                String jsonResponse = objectMapper.writeValueAsString(
                    new ErrorObject(request, message, HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS));
                outputStream.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
            });
    }
}
