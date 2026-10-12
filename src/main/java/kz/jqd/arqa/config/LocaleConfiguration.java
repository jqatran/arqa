package kz.jqd.arqa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;

@Configuration
public class LocaleConfiguration {

    /** The bean name is fixed by Spring MVC: it is how the DispatcherServlet finds the resolver. */
    @Bean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
    public LocaleResolver localeResolver() {
        return new SupportedLanguageLocaleResolver();
    }
}
