package rs.teslaris.core.util;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import rs.teslaris.core.util.exceptionhandling.exception.IdentifierException;

public class IdentifierUtil {

    public static void validateAndSetIdentifier(
        String identifier,
        Integer entityId,
        String pattern,
        BiPredicate<String, Integer> existenceCheck,
        Consumer<String> setter,
        String formatError,
        String existsError
    ) {
        if (Objects.nonNull(identifier)) {
            if (identifier.isBlank()) {
                setter.accept("");
                return;
            }

            var compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            if (compiledPattern.matcher(identifier).matches()) {
                if (existenceCheck.test(identifier, entityId)) {
                    throw new IdentifierException(existsError);
                }
                setter.accept(identifier);
            } else {
                throw new IdentifierException(formatError);
            }
        }
    }
}
