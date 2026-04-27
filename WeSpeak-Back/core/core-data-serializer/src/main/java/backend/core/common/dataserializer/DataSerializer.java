package backend.cowrite.common.dataserializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@NoArgsConstructor
public class DataSerializer {

    private static final ObjectMapper objectMapper = initialize();

    private static ObjectMapper initialize() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule()) // Jackson에 Java8 날짜 시간 타입인 LocalDateTime, LocalDate, Instant 등을 어떻게 (de)serialize 알려주는 모듈 등록
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Json으로 들어온 데이터의 특정 필드가 들어오지 않았어도 오류를 발생시키지 않도록 등록
    }

    public static <T> T deserialize(String data, Class<T> clazz) { //String data -> Class<T> 형태로 변환
        try {
            return objectMapper.readValue(data, clazz);
        } catch (JsonProcessingException e) {
            log.error("[DataSerializer.deserialize] data = {}, clazz ={}", data, clazz, e);
            return null;
        }
    }

    public static <T> T deserialize(Object data, Class<T> clazz) {
        return objectMapper.convertValue(data, clazz);
    }

    public static String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("[DataSerializer.serialize] object ={}", object, e);
            return null;
        }
    }
}
