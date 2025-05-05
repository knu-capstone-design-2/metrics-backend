package kr.cs.interdata.producer.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private static final String HOST_TOPIC = "monitoring.host.dev.json";
    private static final String CONTAINER_TOPIC = "monitoring.container.dev.json";

    /** type에 따라 topic 분류 **/
    public void routeMessageBasedOnType(String jsonPayload) {
        JsonObject jsonObject = new JsonParser().parse(jsonPayload).getAsJsonObject();

        String type = jsonObject.get("type").getAsString();

        if ("host".equalsIgnoreCase(type)) {
            sendToHostTopic(jsonPayload);
        } else if ("container".equalsIgnoreCase(type)) {
            sendToContainerTopic(jsonPayload);
        } else {
            //
        }
    }

    /** host topic으로 전송 **/
    public void sendToHostTopic(String message) {
        kafkaTemplate.send(HOST_TOPIC, message);
    }

    /** container topic으로 전송 **/
    public void sendToContainerTopic(String message) {
        kafkaTemplate.send(CONTAINER_TOPIC, message);
    }
}