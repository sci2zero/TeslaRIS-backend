package rs.teslaris.exporter.util.push;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;

/**
 * Makes a stable key from a free-text name, so that the same name gives the same URI.
 * The key uses the English content, or else the content with the highest priority.
 */
public final class SharedNodeKey {

    private SharedNodeKey() {
    }

    public static Optional<String> of(List<ExportMultilingualContent> names) {
        if (Objects.isNull(names)) {
            return Optional.empty();
        }

        var withText = names.stream()
            .filter(name -> Objects.nonNull(name.getContent()) && !name.getContent().isBlank())
            .toList();

        return withText.stream()
            .filter(name -> "EN".equalsIgnoreCase(name.getLanguageTag()))
            .findFirst()
            .or(() -> withText.stream().max(
                Comparator.comparingInt(ExportMultilingualContent::getPriority)
                    .thenComparing(ExportMultilingualContent::getContent,
                        Comparator.reverseOrder())))
            .map(name -> sha1(normalise(name.getContent())));
    }

    static String normalise(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT)
            .trim()
            .replaceAll("\\s+", " ");
    }

    private static String sha1(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-1")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available.", e);
        }
    }
}
