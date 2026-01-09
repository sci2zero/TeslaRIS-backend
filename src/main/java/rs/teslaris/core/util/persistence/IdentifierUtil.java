package rs.teslaris.core.util.persistence;

import java.net.URI;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rs.teslaris.core.util.exceptionhandling.exception.IdentifierException;
import rs.teslaris.core.util.functional.BiPredicate;

@Component
@Slf4j
public class IdentifierUtil {

    public static String identifierPrefix;

    public static String legacyIdentifierPrefix;


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
        if (Objects.isNull(dtoUriSet)) {
            return;
        }

        for (String str : dtoUriSet) {
            if (Objects.isNull(str) || str.length() > 2048) {
                continue;
            }

            try {
                var uri = URI.create(str);

                if ("http".equalsIgnoreCase(uri.getScheme()) ||
                    "https".equalsIgnoreCase(uri.getScheme())) {
                    uriSet.add(str);
                }
            } catch (IllegalArgumentException ex) {
                // invalid URI -> ignore
                log.warn("Invalid URI save attempted. Exception: {}", ex.getMessage());
            }
        }
    }

    public static String removeCommonPrefix(String localIdentifier) {
        return localIdentifier.trim().replace(IdentifierUtil.identifierPrefix, "");
    }

    @Value("${export.internal-identifier.prefix}")
    public void setIdentifierPrefix(String prefix) {
        IdentifierUtil.identifierPrefix = prefix;
    }

    @Value("${legacy-identifier.prefix}")
    public void setLegacyIdentifierPrefix(String prefix) {
        IdentifierUtil.legacyIdentifierPrefix = prefix;
    }
}
