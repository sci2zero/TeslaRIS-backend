package rs.teslaris.core.util.search;

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
}
