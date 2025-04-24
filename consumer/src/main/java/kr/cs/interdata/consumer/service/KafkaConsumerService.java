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

            if (!isValidJson(json)) continue;

            try {
                JsonNode rootNode = objectMapper.readTree(json);
                JsonNode metricsNode = rootNode.get("metrics");
                JsonNode containerIdNode = rootNode.get("containerId");

                // websocket으로 데이터 전달
                /*
                코드 삽입 필요
                배포 완료 및 도메인 생성 및 프론트의 설정이 어느정도 완료되면 코드 삽입 예정
                 */

                // 로그 출력
                logger.info(record.toString());
            } catch (Exception e) {
                logger.error("JSON 처리 중 오류 발생: {}", e.getMessage());
            }
        }

        // 수동 커밋
        ack.acknowledge();
    }

    /**
     * 	- message(kafka value)의 json 형태를 판별하는 메소드
     *
     * @param message // kafka topic에서 받아온 레코드의 value값
     * @return true : json 형태에 적합, false : json 형태에 부적합
     */
    private boolean isValidJson(String message) {
        try {
            objectMapper.readTree(message);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}