package rs.teslaris.core.util.exceptionhandling.exception;

import java.util.Map;
import lombok.Getter;

@Getter
public class PolicyValidationException extends RuntimeException {

    private final Map<String, String> violations;


    public PolicyValidationException(String message, Map<String, String> violations) {
        super(message);
        this.violations = violations;
    }
}
