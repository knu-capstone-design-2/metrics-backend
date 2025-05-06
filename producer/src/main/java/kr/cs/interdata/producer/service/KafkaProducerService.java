package kr.cs.interdata.producer.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String HOST_TOPIC = "monitoring.host.dev.json";
    private static final String CONTAINER_TOPIC = "monitoring.container.dev.json";

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /** type에 따라 topic 분류 **/
    public void routeMessageBasedOnType(String jsonPayload) {
        JsonObject jsonObject = JsonParser.parseString(jsonPayload).getAsJsonObject();
        String type = jsonObject.get("type").getAsString().toLowerCase();

        String topic = type.equals("host") ? HOST_TOPIC : CONTAINER_TOPIC;

        if (topic != null) {
            kafkaTemplate.send(topic, jsonPayload);
            log.info("Message sent to topic {}", topic);
        } else {
            log.warn("Topic not found for type {}", type);
        }
    }
}