package kz.jqd.arqa;

import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Full application context on an in-memory database, without the startup seed file. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "arqa.seed.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:arqa-test;DB_CLOSE_DELAY=-1"
})
public @interface IntegrationTest {
}
