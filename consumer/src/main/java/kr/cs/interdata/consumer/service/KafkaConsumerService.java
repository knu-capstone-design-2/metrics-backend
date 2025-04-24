package kr.cs.interdata.consumer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.cs.interdata.consumer.infra.ThresholdStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class KafkaConsumerService {

    private final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ThresholdStore thresholdStore;
    private final ThresholdService thresholdService;

    @Autowired
    public KafkaConsumerService(ThresholdStore thresholdStore, ThresholdService thresholdService) {
        this.thresholdStore = thresholdStore;
        this.thresholdService = thresholdService;
    }

    /**
     * 	- Kafka - "host"로부터 배치 메시지를 수신하고 처리하는 메서드
     * 	type : BATCH
     * 	listener Type : BatchMessageListener
     * 	method parameter : onMessage(ConsumerRecords<K, V> data)
     *
     * @param records // 지정 토픽에서 받아온 데이터 list
     */
    @KafkaListener(
            topics = "${KAFKA_TOPIC_HOST}",
            groupId = "${KAFKA_GROUP_ID_STORAGE_GROUP}",
            containerFactory = "customContainerFactory"
    )
    public void batchListenerForHost(ConsumerRecords<String, String> records, Acknowledgment ack) {
        List<Mono<Void>> asyncTasks = new ArrayList<>(); // 비동기 작업을 저장할 리스트

        for (ConsumerRecord<String, String> record : records) {
            String id = record.key();
            String json = record.value();

            try {
                JsonNode rootNode = parseJson(json);  // JSON 파싱을 메서드로 분리
                JsonNode metricsNode = rootNode.get("metrics");


                // WebSocket으로 데이터 전달 예정
                // TODO: 도메인 생성 및 설정 완료 후 삽입 예정

                // timestamp를 LocalDateTime으로 변환
                String timestampStr = rootNode.get("timestamp").asText();
                LocalDateTime violationTime = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_DATE_TIME);

                // metricsNode에서 각 메트릭 데이터를 추출하여 임계값 초과 여부를 확인하고 처리
                metricsNode.fields().forEachRemaining(entry -> {
                    String metricName = entry.getKey(); // 메트릭 이름
                    double metricValue = entry.getValue().asDouble(); // 메트릭 값

                    // thresholdStore에서 해당 메트릭의 임계값을 가져옴
                    Double threshold = thresholdStore.getThreshold(id, metricName);

                    // 임계값이 설정되지 않은 경우(기본값 100.0) 혹은 임계값 초과 시 API로 전송
                    if (threshold != null && metricValue > threshold) {
                        logger.warn("임계값 초과: {} -> {} = {} (기준값: {})", id, metricName, metricValue, threshold);

                        // 임계값 초과 데이터를 API 백엔드로 전송
                        // TODO: API 호출은 대기시간이 있을 수 있으므로, 비동기 작업 처리로 변경 예정
                        thresholdService.sendThresholdViolation(id, metricName, metricValue, violationTime);

                    }
                });

                logger.info("Kafka Record(Host) 처리 성공: {}", record.toString());

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

    /**
     * 	- Kafka - "container"로부터 배치 메시지를 수신하고 처리하는 메서드
     * 	type : BATCH
     * 	listener Type : BatchMessageListener
     * 	method parameter : onMessage(ConsumerRecords<K, V> data)
     *
     * @param records // 지정 토픽에서 받아온 데이터 list
     */
    @KafkaListener(
            topics = "${KAFKA_TOPIC_CONTAINER}",
            groupId = "${KAFKA_GROUP_ID_STORAGE_GROUP}",
            containerFactory = "customContainerFactory"
    )
    public void batchListenerForContainer(ConsumerRecords<String, String> records, Acknowledgment ack) {
        List<Mono<Void>> asyncTasks = new ArrayList<>(); // 비동기 작업을 저장할 리스트

        for (ConsumerRecord<String, String> record : records) {
            String id = record.key();
            String json = record.value();

            try {
                JsonNode rootNode = parseJson(json);  // JSON 파싱을 메서드로 분리
                JsonNode metricsNode = rootNode.get("metrics");


                // WebSocket으로 데이터 전달 예정
                // TODO: 도메인 생성 및 설정 완료 후 삽입 예정

                // timestamp를 LocalDateTime으로 변환
                String timestampStr = rootNode.get("timestamp").asText();
                LocalDateTime violationTime = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_DATE_TIME);

                // metricsNode에서 각 메트릭 데이터를 추출하여,
                // 임계값 초과 여부를 확인하고 처리한다.
                metricsNode.fields().forEachRemaining(entry -> {
                    String metricName = entry.getKey(); // 메트릭 이름
                    double metricValue = entry.getValue().asDouble(); // 메트릭 값

                    // thresholdStore에서 해당 메트릭의 임계값을 가져옴
                    Double threshold = thresholdStore.getThreshold(id, metricName);

                    // 임계값이 설정되지 않은 경우(기본값 100.0) 혹은 임계값 초과 시 API로 전송
                    if (threshold != null && metricValue > threshold) {
                        logger.warn("임계값 초과: {} -> {} = {} (기준값: {})", id, metricName, metricValue, threshold);

                        // 임계값 초과 데이터를 API 백엔드로 전송
                        // TODO: API 호출은 대기시간이 있을 수 있으므로, 비동기 작업 처리로 변경 예정
                        thresholdService.sendThresholdViolation(id, metricName, metricValue, violationTime);

                    }
                });

                logger.info("Kafka Record(Container) 처리 성공: {}", record.toString());

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
    private JsonNode parseJson(String json) {
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