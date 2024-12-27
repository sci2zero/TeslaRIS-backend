package rs.teslaris.core.util.search;

import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Transliterator;
import jakarta.annotation.Nonnull;
import java.util.List;
import org.apache.lucene.queryparser.classic.QueryParserBase;

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
            token -> token.equals("*") || token.equals(".") ? "*" : QueryParserBase.escape(token));
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
}
