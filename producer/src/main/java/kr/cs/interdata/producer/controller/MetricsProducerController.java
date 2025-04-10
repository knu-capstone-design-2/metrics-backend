package kr.cs.interdata.producer.controller;

import kr.cs.interdata.producer.service.KafkaProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/kafka")
public class MetricsProducerController {

    private final KafkaProducerService kafkaProducerService;

    @Autowired
    public MetricsProducerController(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendMetrics(@RequestBody String jsonPayload) {
        kafkaProducerService.routeMessageBasedOnType(jsonPayload);

        return ResponseEntity.ok("Message sent successfully");
    }
}
