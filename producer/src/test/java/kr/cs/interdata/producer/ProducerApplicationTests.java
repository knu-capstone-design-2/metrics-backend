package kr.cs.interdata.producer;

import kr.cs.interdata.producer.service.KafkaProducerService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"monitoring.host.dev.json"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
class ProducerApplicationTests {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Test
    void testSendMessage() {

        // given
        String jsonPayload = "{ \"type\": \"host\", \"cpu\": 45, \"memory\": 1024 }";

        // when
        kafkaProducerService.routeMessageBasedOnType(jsonPayload);

        // then
    }
}