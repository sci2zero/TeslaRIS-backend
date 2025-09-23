package rs.teslaris.core.util.persistence;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import rs.teslaris.core.util.exceptionhandling.exception.IdentifierException;
import rs.teslaris.core.util.functional.BiPredicate;

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
            if (identifier.isBlank() || identifier.equals("NONE")) {
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
        } else {
            setter.accept("");
        }
    }

    public static void setUris(Set<String> uriSet, Set<String> dtoUriSet) {
        var uriPattern =
            "^(?:(?:http|https)://)(?:\\S+(?::\\S*)?@)?(?:(?:(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[0-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)(?:\\.(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)*(?:\\.(?:[a-z\\u00a1-\\uffff]{2,})))|localhost)(?::\\d{2,5})?(?:(/|\\?|#)[^\\s]*)?$";
        var pattern = Pattern.compile(uriPattern, Pattern.CASE_INSENSITIVE);

        if (Objects.nonNull(dtoUriSet)) {
            dtoUriSet.forEach(str -> {
                if (str.length() <= 2048 && pattern.matcher(str).matches()) {
                    uriSet.add(str);
                }
            });
        }
    }
}
