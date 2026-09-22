package rs.teslaris.revisioner.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ObjectMapperProvider {

    private static final ObjectMapper objectMapper =
        JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .addModule(new JavaTimeModule())
            .build();

    public static ObjectMapper provideObjectmapper() {
        return objectMapper;
    }
}
