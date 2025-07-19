package rs.teslaris.core.util.language;

import java.util.Locale;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

public class TransliterationMessageSource implements MessageSource {

    private final MessageSource delegate;

    public TransliterationMessageSource(MessageSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getMessage(@NotNull String code, Object[] args, String defaultMessage,
                             @NotNull Locale locale) {
        String message = delegate.getMessage(code, args, defaultMessage, resolveLocale(locale));
        return maybeTransliterate(message, locale);
    }

    @NotNull
    @Override
    public String getMessage(@NotNull String code, Object[] args, @NotNull Locale locale)
        throws NoSuchMessageException {
        String message = delegate.getMessage(code, args, resolveLocale(locale));
        return maybeTransliterate(message, locale);
    }

    @NotNull
    @Override
    public String getMessage(
        @NotNull org.springframework.context.MessageSourceResolvable resolvable,
        @NotNull Locale locale)
        throws NoSuchMessageException {
        String message = delegate.getMessage(resolvable, resolveLocale(locale));
        return maybeTransliterate(message, locale);
    }

    private Locale resolveLocale(Locale original) {
        if (Objects.nonNull(original) && "sr-cyr".equalsIgnoreCase(original.toLanguageTag()) ||
            "sr-cyr".equalsIgnoreCase(Objects.requireNonNull(original).getLanguage())) {
            return Locale.forLanguageTag("sr");
        }
        return original;
    }

    private String maybeTransliterate(String message, Locale originalLocale) {
        if (Objects.nonNull(originalLocale) &&
            "sr-cyr".equalsIgnoreCase(originalLocale.toLanguageTag()) ||
            "sr-cyr".equalsIgnoreCase(Objects.requireNonNull(originalLocale).getLanguage())) {
            return SerbianTransliteration.toCyrillic(message);
        }
        return message;
    }
}

