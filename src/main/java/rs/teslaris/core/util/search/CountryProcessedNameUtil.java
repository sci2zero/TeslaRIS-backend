package rs.teslaris.core.util.search;

import jakarta.annotation.Nullable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import rs.teslaris.core.model.commontypes.Country;

/**
 * Builds the processed name of a country: every name of it, its code and the three letter code of
 * that code, all folded to lower case ASCII.
 */
public class CountryProcessedNameUtil {

    private CountryProcessedNameUtil() {
    }

    public static String build(Country country) {
        var processedName = new StringBuilder();

        country.getName().forEach(name -> processedName.append(" ")
            .append(StringUtil.performSimpleLatinPreprocessing(name.getContent())));

        processedName.append(" ")
            .append(StringUtil.performSimpleLatinPreprocessing(country.getCode()));

        var threeLetterCode = threeLetterCode(country.getCode());

        if (Objects.nonNull(threeLetterCode)) {
            processedName.append(" ").append(threeLetterCode);
        }

        return processedName.toString();
    }

    /**
     * The ISO 3166-1 alpha-3 code of a two letter code, or {@code null} for a code that has none.
     */
    @Nullable
    private static String threeLetterCode(String code) {
        if (Objects.isNull(code) || code.isBlank()) {
            return null;
        }

        try {
            var threeLetterCode = Locale.of("", code).getISO3Country();

            return threeLetterCode.isBlank() ? null : threeLetterCode.toLowerCase();
        } catch (MissingResourceException e) {
            // A code outside the standard, "YU" for instance, has no three letter form.
            return null;
        }
    }
}
