package rs.teslaris.revisioner.util.fixators;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

/**
 * The typographic characters harvested metadata arrives with, mapped to their plain equivalents.
 */
public final class TypographicCharacterRules {

    private static final Map<Character, String> REPLACEMENTS = new HashMap<>();

    private static final Map<Character, Character> SPACING_DIACRITICS = Map.of(
        (char) 0x02DC, (char) 0x0303,
        (char) 0x02C6, (char) 0x0302,
        (char) 0x00B4, (char) 0x0301,
        (char) 0x00A8, (char) 0x0308,
        (char) 0x00B8, (char) 0x0327,
        (char) 0x02CB, (char) 0x0300);

    private static final String COMPOSABLE_LETTERS = "ãõÃÕáéíóúÁÉÍÓÚâêôÂÊÔàÀçÇüÜ";

    private static final Pattern HORIZONTAL_WHITESPACE = Pattern.compile("[ \\t]+");

    static {
        map("'", '’', '‘', '‚', '‛', '′', 'ʼ', '´', '`');
        map("\"", '“', '”', '„', '‟', '″', '«', '»');
        map("-", '–', '—', '‒', '―', '−', '‐', '‑');
        map("...", '…');
        map("fi", 'ﬁ');
        map("fl", 'ﬂ');
        map("ff", 'ﬀ');
        map("ffi", 'ﬃ');
        map("ffl", 'ﬄ');
        map(" ", (char) 0x00A0, (char) 0x2002, (char) 0x2003, (char) 0x2007, (char) 0x2009,
            (char) 0x202F, (char) 0x3000);
        map("", (char) 0x00AD, (char) 0x200B, (char) 0x200C, (char) 0x200D, (char) 0xFEFF);
    }


    private TypographicCharacterRules() {
    }

    /**
     * The text with every known typographic character replaced and its horizontal whitespace
     * collapsed, or the text untouched when normalising would leave nothing behind.
     */
    public static String normalise(String text) {
        if (Objects.isNull(text) || text.isBlank()) {
            return text;
        }

        var recombined =
            recombineSpacingDiacritics(Normalizer.normalize(text, Normalizer.Form.NFC));
        var replaced = new StringBuilder(recombined.length());

        for (var character : recombined.toCharArray()) {
            var replacement = REPLACEMENTS.get(character);
            replaced.append(Objects.nonNull(replacement) ? replacement : character);
        }

        var normalised = HORIZONTAL_WHITESPACE.matcher(replaced).replaceAll(" ").strip();
        return normalised.isBlank() ? text : normalised;
    }

    public static void normalise(List<MultilingualContentDTO> multilingualContent) {
        if (Objects.isNull(multilingualContent)) {
            return;
        }

        multilingualContent.stream().filter(Objects::nonNull)
            .forEach(content -> content.setContent(normalise(content.getContent())));
    }

    /**
     * Puts a spacing diacritic a PDF or LaTeX extraction left in front of its letter back onto it,
     * so that the accent is not read as an apostrophe further down.
     */
    private static String recombineSpacingDiacritics(String text) {
        var recombined = new StringBuilder(text.length());

        for (var index = 0; index < text.length(); index++) {
            var combiningMark = SPACING_DIACRITICS.get(text.charAt(index));

            if (Objects.nonNull(combiningMark) && index + 1 < text.length()) {
                var candidate = Normalizer.normalize(
                    String.valueOf(text.charAt(index + 1)) + combiningMark, Normalizer.Form.NFC);

                if (candidate.length() == 1 &&
                    COMPOSABLE_LETTERS.indexOf(candidate.charAt(0)) >= 0) {
                    recombined.append(candidate);
                    index++;
                    continue;
                }
            }

            recombined.append(text.charAt(index));
        }

        return recombined.toString();
    }

    private static void map(String replacement, char... characters) {
        for (var character : characters) {
            REPLACEMENTS.put(character, replacement);
        }
    }
}
