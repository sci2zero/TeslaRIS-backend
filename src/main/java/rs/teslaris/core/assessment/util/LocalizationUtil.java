package rs.teslaris.core.assessment.util;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class LocalizationUtil {
    private static MessageSource messageSource;

    public LocalizationUtil(MessageSource source) {
        LocalizationUtil.messageSource = source;
    }

    public static String getMessage(String key, Object[] params, String locale) {
        return messageSource.getMessage(key, params, Locale.forLanguageTag(locale));
    }
}
