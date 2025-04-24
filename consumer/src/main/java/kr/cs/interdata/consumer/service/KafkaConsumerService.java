package kr.cs.interdata.consumer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaConsumerService {

    private final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();


    /**
     * 	- Kafka로부터 배치 메시지를 수신하고 처리하는 메서드
     * 	type : BATCH
     * 	listener Type : BatchMessageListener
     * 	method parameter : onMessage(ConsumerRecords<K, V> data)
     *
     * @param records // 지정 토픽에서 받아온 데이터 list
     */
    @KafkaListener(
            topics = "${KAFKA_TOPIC_METRICS}",
            groupId = "${KAFKA_GROUP_ID_STORAGE_GROUP}",
            containerFactory = "customContainerFactory"
    )
    public void batchListener(ConsumerRecords<String, String> records, Acknowledgment ack) {
        for (ConsumerRecord<String, String> record : records) {
            String id = record.key();
            String json = record.value();

            try {
                JsonNode rootNode = parseJson(json, record);  // JSON 파싱을 메서드로 분리
                JsonNode metricsNode = rootNode.get("metrics");
                JsonNode containerIdNode = rootNode.get("containerId");

                if (metricsNode == null || containerIdNode == null) {
                    throw new IllegalArgumentException("필수 필드(metrics 또는 containerId)가 누락된 메시지입니다.");
                }

                // WebSocket으로 데이터 전달 예정
                // TODO: 도메인 생성 및 프론트 설정 완료 후 삽입 예정

                logger.info("Kafka Record 처리 성공: {}", record.toString());

            } catch (InvalidJsonException e) {
                logger.error("잘못된 JSON 형식 - key: {}, value: {}, error: {}", record.key(), record.value(), e.getMessage());
            } catch (IllegalArgumentException e) {
                logger.warn("JSON 필드 누락 - key: {}, value: {}, 원인: {}", record.key(), record.value(), e.getMessage());
            } catch (Exception e) {
                logger.error("예상치 못한 예외 발생 - key: {}, value: {}", record.key(), record.value(), e);
            }

        }

        // 수동 커밋
        ack.acknowledge();
    }

    // json 파싱
    private JsonNode parseJson(String json, ConsumerRecord<String, String> record) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new InvalidJsonException("JSON 파싱 실패", e);
        }
    }
    
    // 사용자 정의 예외
    public static class InvalidJsonException extends RuntimeException {
        public InvalidJsonException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}