package kz.jqd.arqa.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards the translations: no language may lack a message that another language has. */
class MessageBundlesTest {

    private static Properties load(String fileName) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = new ClassPathResource(fileName).getInputStream()) {
            properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        }
        return properties;
    }

    private static Set<String> keys(Properties properties) {
        return new TreeSet<>(properties.stringPropertyNames());
    }

    private static int placeholderCount(String message) {
        return new MessageFormat(message).getFormatsByArgumentIndex().length;
    }

    @Test
    void russianAndKazakhHaveExactlyTheKeysOfTheEnglishBundle() throws IOException {
        Set<String> englishKeys = keys(load("messages.properties"));

        assertThat(keys(load("messages_ru.properties"))).isEqualTo(englishKeys);
        assertThat(keys(load("messages_kk.properties"))).isEqualTo(englishKeys);
    }

    @Test
    void everyMessageIsFilledInAndKeepsTheSamePlaceholdersAsEnglish() throws IOException {
        Properties english = load("messages.properties");
        for (String fileName : new String[]{"messages_ru.properties", "messages_kk.properties"}) {
            Properties translated = load(fileName);
            for (String key : english.stringPropertyNames()) {
                assertThat(translated.getProperty(key)).as(fileName + " / " + key).isNotBlank();
                assertThat(placeholderCount(translated.getProperty(key)))
                        .as(fileName + " / placeholders of " + key)
                        .isEqualTo(placeholderCount(english.getProperty(key)));
            }
        }
    }
}
