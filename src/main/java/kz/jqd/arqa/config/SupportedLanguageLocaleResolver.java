package kz.jqd.arqa.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Picks the response language: the {@code lang} query parameter if it names a supported language,
 * otherwise the {@code Accept-Language} header, otherwise English.
 * Supported languages are English, Russian and Kazakh; anything else falls back to English.
 */
public class SupportedLanguageLocaleResolver extends AcceptHeaderLocaleResolver {

    public static final String LANGUAGE_PARAMETER = "lang";

    private static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.ENGLISH, Locale.of("ru"), Locale.of("kk"));

    public SupportedLanguageLocaleResolver() {
        setSupportedLocales(SUPPORTED_LOCALES);
        setDefaultLocale(Locale.ENGLISH);
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String requestedLanguage = request.getParameter(LANGUAGE_PARAMETER);
        if (requestedLanguage != null) {
            Locale requestedLocale = Locale.forLanguageTag(requestedLanguage.trim());
            for (Locale supportedLocale : SUPPORTED_LOCALES) {
                if (supportedLocale.getLanguage().equals(requestedLocale.getLanguage())) {
                    return supportedLocale;
                }
            }
        }
        return super.resolveLocale(request);
    }
}
