package rs.teslaris.core.util.persistence;

import jakarta.annotation.Nullable;
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
import rs.teslaris.core.util.migration.MigrationContext;
import rs.teslaris.core.util.search.CollectionOperations;

@Component
@Slf4j
public class IdentifierUtil {

    public static final String ID_PLACEHOLDER = "{ID}";
    private static final Pattern URL_TEMPLATE_PATTERN =
        Pattern.compile("^https?://\\S+$", Pattern.CASE_INSENSITIVE);
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

            // Migrated records are known to be dirty, and refusing them over a malformed or
            // duplicated identifier would lose the record entirely. The value is kept as it came.
            if (MigrationContext.isActive()) {
                setter.accept(identifier);
                return;
            }

            var compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            if (compiledPattern.matcher(identifier).matches()) {
                if (existenceCheck.test(identifier, entityId)) {
                    throw new IdentifierException(existsError);
                }
                setter.accept(identifier);
            } else {
                log.warn("Identifier '{}' failed format check ({}).", identifier, formatError);
                throw new IdentifierException(formatError);
            }
        } else {
            setter.accept("");
        }
    }

    public static void setUris(Set<String> uriSet, Set<String> dtoUriSet) {
        uriSet.clear();

        if (!CollectionOperations.containsValues(dtoUriSet)) {
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

    public static boolean isResolvableUrlTemplate(String urlTemplate) {
        return Objects.nonNull(urlTemplate) && urlTemplate.contains(ID_PLACEHOLDER);
    }

    /**
     * Resolvable URL for an identifier value, or null when the template cannot produce one.
     */
    @Nullable
    public static String buildIdentifierUrl(String urlTemplate, String value) {
        if (!isResolvableUrlTemplate(urlTemplate) || Objects.isNull(value) || value.isBlank()) {
            return null;
        }

        return urlTemplate.trim().replace(ID_PLACEHOLDER, value.trim());
    }

    public static void validateUrlTemplate(String urlTemplate) {
        if (Objects.isNull(urlTemplate) || urlTemplate.isBlank()) {
            return;
        }

        var trimmed = urlTemplate.trim();

        if (!trimmed.contains(ID_PLACEHOLDER)) {
            throw new IdentifierException(
                "URL template has to contain the " + ID_PLACEHOLDER + " placeholder.");
        }

        if (!URL_TEMPLATE_PATTERN.matcher(trimmed.replace(ID_PLACEHOLDER, "1")).matches()) {
            throw new IdentifierException("URL template has to be an HTTP or HTTPS URL.");
        }
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
