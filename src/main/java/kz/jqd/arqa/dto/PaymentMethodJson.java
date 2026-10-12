package kz.jqd.arqa.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import kz.jqd.arqa.model.PaymentMethod;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.util.Locale;

/**
 * JSON spelling of {@link PaymentMethod}: lower case ("cash", "card"), as in the data format of the task.
 * Kept in the web layer so the domain enum does not depend on Jackson.
 */
@JsonComponent
public class PaymentMethodJson {

    public static class Serializer extends JsonSerializer<PaymentMethod> {
        @Override
        public void serialize(PaymentMethod paymentMethod, JsonGenerator generator, SerializerProvider provider)
                throws IOException {
            generator.writeString(paymentMethod.name().toLowerCase(Locale.ROOT));
        }
    }

    public static class Deserializer extends JsonDeserializer<PaymentMethod> {
        @Override
        public PaymentMethod deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String jsonValue = parser.getValueAsString();
            try {
                return PaymentMethod.valueOf(jsonValue.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return (PaymentMethod) context.handleWeirdStringValue(PaymentMethod.class, jsonValue,
                        "expected 'cash' or 'card'");
            }
        }
    }
}
