package rs.teslaris.core.util.language;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class LocalizedTokenResolver {

    private static final Pattern TOKEN = Pattern.compile("^[a-z][a-zA-Z]*\\.[A-Z][A-Z0-9_]*$");

    private static MessageSource messageSource;


    public LocalizedTokenResolver(MessageSource messageSource) {
        LocalizedTokenResolver.messageSource = messageSource;
    }

    public static boolean isToken(Object value) {
        return value instanceof String text && TOKEN.matcher(text).matches();
    }

    /**
     * @return the localized form of the token, or the token itself when the bundle has no entry for
     * it, so a missing translation degrades to something readable rather than failing the message
     */
    public static String resolve(String token, String languageCode) {
        if (Objects.isNull(messageSource)) {
            return token;
        }

        try {
            return messageSource.getMessage(token, null, Locale.forLanguageTag(languageCode));
        } catch (Exception e) {
            return token;
        }
    }
}
