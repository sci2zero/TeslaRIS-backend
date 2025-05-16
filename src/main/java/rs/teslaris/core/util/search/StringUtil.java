package rs.teslaris.core.util.search;

import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Transliterator;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

public class StringUtil {

    public static void removeTrailingDelimiters(StringBuilder contentSr,
                                                StringBuilder contentOther) {
        removeTrailingDelimitersHelper(contentSr);
        removeTrailingDelimitersHelper(contentOther);
    }

    private static void removeTrailingDelimitersHelper(StringBuilder content) {
        String[] delimiters = {" | ", " | ;", " | ; ", "; "};

        for (String delimiter : delimiters) {
            if (content.toString().endsWith(delimiter)) {
                content.delete(content.length() - delimiter.length(), content.length());
                break;
            }
        }
    }

    public static String removeLeadingColonSpace(String content) {
        StringBuilder stringBuilder = new StringBuilder(content);
        if (stringBuilder.toString().startsWith("; ")) {
            stringBuilder.delete(0, 2);
        }
        return stringBuilder.toString();
    }

    public static void sanitizeTokens(List<String> tokens) {
        tokens.replaceAll(
            token -> (token.equals("*") || token.equals(".")) ? "*" :
                QueryParserBase.escape(token));
    }

    @Nonnull
    public static String performSimpleSerbianPreprocessing(String input) {
        if (input == null) {
            return "";
        }

        var transliterator =
            Transliterator.getInstance("Any-Latin; NFD; [:Nonspacing Mark:] Remove; NFC");

        var result = transliterator.transliterate(input.toLowerCase());

        var normalizer = Normalizer2.getNFDInstance();
        result = normalizer.normalize(result);

        result = result.replaceAll("[^a-z0-9 ]", "");

        result = result.toLowerCase();

        return result;
    }

    public static String formatIssn(String issn) {
        if (issn == null || issn.isEmpty()) {
            return "";
        }

        if (issn.length() < 8) {
            return "";
        }

        if (issn.contains("-")) {
            return issn;
        }

        return issn.substring(0, 4) + "-" + issn.substring(4, 8);
    }

    public static String getStringContent(Set<MultiLingualContent> multilingualContent,
                                          String lang) {
        MultiLingualContent fallback = null;
        for (var content : multilingualContent) {
            if (lang.equalsIgnoreCase(content.getLanguage().getLanguageTag())) {
                return content.getContent();
            }
            fallback = content;
        }
        return fallback != null ? fallback.getContent() : "";
    }

    public static boolean isInteger(String s, int radix) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) {
                    return false;
                } else {
                    continue;
                }
            }
            if (Character.digit(s.charAt(i), radix) < 0) {
                return false;
            }
        }
        return true;
    }
}
