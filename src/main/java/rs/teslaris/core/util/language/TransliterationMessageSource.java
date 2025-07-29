package rs.teslaris.core.util.language;

import java.util.Locale;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

public class TransliterationMessageSource implements MessageSource {

    private static final Locale SR_CYRILLIC =
        new Locale.Builder()
            .setLanguage("sr")
            .setScript("Cyrl")
            .build();

    private static final Locale SR_LOCALE =
        Locale.forLanguageTag(LanguageAbbreviations.SERBIAN);

    private final MessageSource delegate;


    public TransliterationMessageSource(MessageSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getMessage(@NotNull String code, Object[] args, String defaultMessage,
                             @NotNull Locale locale) {
        if (isCyrillicLocale(locale)) {
            try {
                return delegate.getMessage(code + ".cyrl", args, resolveLocale(SR_LOCALE));
            } catch (NoSuchMessageException e) {
                var fallback =
                    delegate.getMessage(code, args, defaultMessage, resolveLocale(locale));
                return SerbianTransliteration.toCyrillic(fallback);
            }
        }
        return delegate.getMessage(code, args, defaultMessage, locale);
    }

    @NotNull
    @Override
    public String getMessage(@NotNull String code, Object[] args, @NotNull Locale locale)
        throws NoSuchMessageException {

        if (isCyrillicLocale(locale)) {
            try {
                return delegate.getMessage(code + ".cyrl", args, resolveLocale(SR_LOCALE));
            } catch (NoSuchMessageException e) {
                var fallback = delegate.getMessage(code, args, resolveLocale(locale));
                return SerbianTransliteration.toCyrillic(fallback);
            }
        }
        return delegate.getMessage(code, args, locale);
    }

    @NotNull
    @Override
    public String getMessage(@NotNull MessageSourceResolvable resolvable, @NotNull Locale locale)
        throws NoSuchMessageException {

        if (isCyrillicLocale(locale)) {
            for (var code : Objects.requireNonNull(resolvable.getCodes())) {
                try {
                    return delegate.getMessage(code + ".cyrl", resolvable.getArguments(),
                        resolveLocale(SR_LOCALE));
                } catch (NoSuchMessageException ignored) {
                    // Try next code
                }
            }
            var fallback = delegate.getMessage(resolvable, resolveLocale(locale));
            return SerbianTransliteration.toCyrillic(fallback);
        }
        return delegate.getMessage(resolvable, locale);
    }

    private boolean isCyrillicLocale(Locale locale) {
        if (Objects.isNull(locale)) {
            return false;
        }

        if (locale.toLanguageTag().endsWith("cyr")) {
            locale = SR_CYRILLIC;
        }

        var script = locale.getScript();
        if ("Cyrl".equalsIgnoreCase(script)) {
            return true;
        }

        var tag = locale.toLanguageTag().toLowerCase();
        return tag.equals("sr-x-lvariant-cyrl") || tag.equals("sr-cyrl") || tag.equals("sr-cyr");
    }

    private Locale resolveLocale(Locale original) {
        if (isCyrillicLocale(original)) {
            return SR_LOCALE;
        }

        return original;
    }
}
