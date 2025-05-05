package kr.cs.interdata.producer;

import kr.cs.interdata.producer.service.KafkaProducerService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ProducerApplicationTests {

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    private static final String HOST_TOPIC = "monitoring.host.dev.json";
    private static final String CONTAINER_TOPIC = "monitoring.container.dev.json";

    @Test
    @DisplayName("kafka producer sends message to host topic")
    void testSendToHostTopic() {
        String jsonPayload = "{\"type\":\"host\"}";
        kafkaProducerService.routeMessageBasedOnType(jsonPayload);
        verify(kafkaTemplate, times(1)).send(eq(CONTAINER_TOPIC), eq(jsonPayload));
    }

    @Test
    @DisplayName("kafka producer sends message to container topic")
    void testSendToContainerTopic() {
//
    }

    @Test
    @DisplayName("type error")
    void testTypeError() {
//
    }

}