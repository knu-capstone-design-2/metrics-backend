package kr.cs.interdata.producer.runner;

import kr.cs.interdata.producer.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaMessageTrigger {

    private final KafkaProducerService kafkaProducerService;

    @Autowired
    public KafkaMessageTrigger(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    /***
     *
     * @param jsonPayload
     *
     * data-collector가 JSON을 보낼 때마다 호출
     */
    public void processJsonMessage(String jsonPayload) {
        kafkaProducerService.routeMessageBasedOnType(jsonPayload);
    }
}
