package rs.teslaris.core.util.search;

import java.util.List;
import org.apache.lucene.queryparser.classic.QueryParserBase;

public class StringUtil {

    public static void removeTrailingPipeDelimiter(StringBuilder contentSr,
                                                   StringBuilder contentOther) {
        if (contentSr.toString().endsWith(" | ")) {
            contentSr.delete(contentSr.length() - 3, contentSr.length());
        }
        if (contentOther.toString().endsWith(" | ")) {
            contentOther.delete(contentOther.length() - 3, contentOther.length());
        }
    }

    public static void sanitizeTokens(List<String> tokens) {
        tokens.replaceAll(token -> token.equals("*") ? token : QueryParserBase.escape(token));
    }
}
