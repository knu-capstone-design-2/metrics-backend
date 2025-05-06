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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
     * @param records   지정 토픽에서 받아온 데이터 list
     */
    @KafkaListener(
            topics = "${KAFKA_TOPIC_HOST}",
            groupId = "${KAFKA_GROUP_ID_STORAGE_GROUP}",
            containerFactory = "customContainerFactory"
    )
    public void batchListenerForHost(ConsumerRecords<String, String> records, Acknowledgment ack) {
        List<Mono<Void>> asyncTasks = new ArrayList<>(); // 비동기 작업을 저장할 리스트

        for (ConsumerRecord<String, String> record : records) {
            // TODO: Producer에서 key값을 입력해 데이터를 넘겨주는지 확인하기
            //String id = record.key();
            String json = record.value();

            try {
                JsonNode metricsNode = parseJson(json);

                // *******************************
                //      transmit to websocket
                // *******************************
                // WebSocket으로 데이터 전달 예정
                // TODO: 도메인 생성 및 설정 완료 후 삽입 예정


                // ***********************
                //      set timestamp
                // ***********************
                LocalDateTime violationTime = LocalDateTime.now();


                // ***************************
                //      compare threshold
                // ***************************
                double metricValue = 0.0;
                String metricName = null;

                // type ID
                String typeId = null;
                if (metricsNode.has("hostId")) {
                    typeId = metricsNode.get("hostId").asText();
                }
                else {
                    logger.warn("존재하지 않는 id입니다.");
                }

                // CPU, Memory, Disk
                for (int i = 0;i < 3;i++){
                    if (i == 0) {
                        metricName = "cpu";
                        metricValue = metricsNode.get("cpuUsagePercent").asDouble();
                    }
                    if (i == 1) {
                        metricName = "memory";
                        double memoryTotalBytes = metricsNode.get("memoryTotalBytes").asDouble();
                        double memoryUsedBytes = metricsNode.get("memoryUsedBytes").asDouble();
                        metricValue = (memoryUsedBytes * 100) / memoryTotalBytes;
                    }
                    if (i == 2) {
                        metricName = "disk";
                        double diskReadBytesRate = metricsNode.get("diskReadBytes").asDouble();
                        double diskWriteBytesRate = metricsNode.get("diskWriteBytesRate").asDouble();
                        metricValue = diskReadBytesRate + diskWriteBytesRate;
                    }

                    // 각 메트릭별 threshold를 조회해 초과하면 DB에 저장 후, 로깅함.
                    processThreshold("host", typeId,
                            metricName, metricValue, violationTime);
                }

                // Network
                JsonNode networkNode = metricsNode.path("network");

                metricName = "network";
                if (!networkNode.isMissingNode() && networkNode.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> interfaces = networkNode.fields();
                    while (interfaces.hasNext()) {
                        Map.Entry<String, JsonNode> entry = interfaces.next();
                        String interfaceName = entry.getKey();
                        JsonNode interfaceData = entry.getValue();

                        double bytesReceivedRate = interfaceData.path("bytesReceived").asDouble();
                        double bytesSentRate = interfaceData.path("bytesSent").asDouble();
                        metricValue = bytesReceivedRate + bytesSentRate;

                        // 각 메트릭별 threshold를 조회해 초과하면 DB에 저장 후, 로깅함.
                        processThreshold("host", typeId,
                                metricName, metricValue, violationTime);
                    }
                } else {
                    logger.warn("Host:{} - network 데이터를 찾을 수 없습니다.", typeId);
                }

                // metricsNode에서 각 메트릭 데이터를 추출하여 임계값 초과 여부를 확인하고 처리
                // 문제가 있음. 데이터 받아오는 건 CPU는 Usage 한개지만 메모리는 free, use, total 세개가 다 옴.
                // 이걸 여기서 하나의 값으로 처리할 수는 있음. -> for each문 안쓰면 됨! 걍 하드코딩식으로 각 값 key값을 다 받아와서 처리하는 식으로
                // 근데 이렇게되면 모니터링 메트릭 값을 추후 추가할 수 없게 됨 -> 솔직히 돌아가는 게 우선순위면 이것도 별 문제가 안됨
                // 일단 메트릭당 하나의 값을 준다고 가정했을 때의 코드로 짜놨는데
                // 추후 채은이가 정말 정확한 json데이터를 정한 후에 하드코딩식으로 받아오게 수정해야 할듯
                //
                // ** cpu랑 memory랑 disk I/O는 하나지만, Network는 그 수가 변칙적임
                // 그래서 네트워크는 key값을 "network"로 받아오고 value를 json데이터로 또 받아오고(network_json)
                // network_json에서 foreach해서 네트워크마다 받아오자
                // 근데 네트워크는 변칙적인만큼 db에 이상 데이터값으로는 어떤 네트워크든 network I/O로 통일되어서 들어갈듯

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
     * @param records   지정 토픽에서 받아온 데이터 list
     */
    @KafkaListener(
            topics = "${KAFKA_TOPIC_CONTAINER}",
            groupId = "${KAFKA_GROUP_ID_STORAGE_GROUP}",
            containerFactory = "customContainerFactory"
    )
    public void batchListenerForContainer(ConsumerRecords<String, String> records, Acknowledgment ack) {
        List<Mono<Void>> asyncTasks = new ArrayList<>(); // 비동기 작업을 저장할 리스트

        for (ConsumerRecord<String, String> record : records) {
            // TODO: Producer에서 key값을 입력해 데이터를 넘겨주는지 확인하기
            //String id = record.key();
            String json = record.value();

            try {
                JsonNode metricsNode = parseJson(json);

                // *******************************
                //      transmit to websocket
                // *******************************
                // WebSocket으로 데이터 전달 예정
                // TODO: 도메인 생성 및 설정 완료 후 삽입 예정


                // ***********************
                //      set timestamp
                // ***********************
                LocalDateTime violationTime = LocalDateTime.now();


                // ***************************
                //      compare threshold
                // ***************************
                double metricValue = 0.0;
                String metricName = null;

                // type ID
                String typeId = null;
                if (metricsNode.has("containerId")) {
                    typeId = metricsNode.get("containerId").asText();
                }
                else {
                    logger.warn("존재하지 않는 id입니다.");
                }

                // CPU, Memory, Disk
                for (int i = 0;i < 3;i++){
                    if (i == 0) {
                        metricName = "cpu";
                        metricValue = metricsNode.get("cpuUsagePercent").asDouble();
                    }
                    if (i == 1) {
                        metricName = "memory";
                        double memoryTotalBytes = metricsNode.get("memoryTotalBytes").asDouble();
                        double memoryUsedBytes = metricsNode.get("memoryUsedBytes").asDouble();
                        metricValue = (memoryUsedBytes * 100) / memoryTotalBytes;
                    }
                    if (i == 2) {
                        metricName = "disk";
                        double diskReadBytesRate = metricsNode.get("diskReadBytes").asDouble();
                        double diskWriteBytesRate = metricsNode.get("diskWriteBytesRate").asDouble();
                        metricValue = diskReadBytesRate + diskWriteBytesRate;
                    }

                    // 각 메트릭별 threshold를 조회해 초과하면 DB에 저장 후, 로깅함.
                    processThreshold("container", typeId,
                            metricName, metricValue, violationTime);
                }

                // Network
                JsonNode networkNode = metricsNode.path("network");

                metricName = "network";
                if (!networkNode.isMissingNode() && networkNode.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> interfaces = networkNode.fields();
                    while (interfaces.hasNext()) {
                        Map.Entry<String, JsonNode> entry = interfaces.next();
                        String interfaceName = entry.getKey();
                        JsonNode interfaceData = entry.getValue();

                        double bytesReceivedRate = interfaceData.path("bytesReceived").asDouble();
                        double bytesSentRate = interfaceData.path("bytesSent").asDouble();
                        metricValue = bytesReceivedRate + bytesSentRate;

                        // 각 메트릭별 threshold를 조회해 초과하면 DB에 저장 후, 로깅함.
                        processThreshold("container", typeId,
                                metricName, metricValue, violationTime);
                    }
                } else {
                    logger.warn("Container:{} - network 데이터를 찾을 수 없습니다.", typeId);
                }
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


    /**
     *  - 각 메트릭별 threshold를 조회한 후, 
     *      초과하면 DB에 저장하는 API를 호출한 다음, 필요하다면 로깅함.
     * 
     * @param type          처리할 데이터의 type ("host" or "container")
     * @param typeId        처리할 데이터의 고유 id(ex. host001, container023)
     * @param metricName    threshold를 비교할 메트릭의 이름(ex. "cpu", "disk",...)
     * @param metricValue   threshold와 비교할 메트릭의 모니터링 값
     * @param violationTime 메트릭을 받아온 시각
     */
    public void processThreshold(String type, String typeId,
                                 String metricName, Double metricValue, LocalDateTime violationTime) {
        // thresholdStore에서 해당 메트릭의 임계값을 가져옴
        Double threshold = thresholdStore.getThreshold(type, metricName);

        // 임계값을 정상적으로 받아오고 임계값 초과 시 API로 전송
        if (threshold != null && metricValue > threshold) {
            logger.warn("임계값 초과: {} -> {} = {} (기준값: {})", type, metricName, metricValue, threshold);

            // 임계값 초과 데이터를 API 백엔드로 전송
            // API 호출은 대기시간이 있을 수 있으므로, sendThresholdViolation함수 내 호출에서 비동기 작업 처리한다.
            thresholdService.sendThresholdViolation(typeId, metricName, metricValue, violationTime);

        }
        // 임계값을 정상적으로 받아오고, 임계값이 초과되지 않음.
        else if (threshold != null && (metricValue < threshold || metricValue.equals(threshold))) {
            // Do nothing (정상값이고, 로깅 하지 않음)
        }
        // 임계값을 정상적으로 받아오지 못함.
        else {
            logger.warn("임계값이 조회되지 않았습니다.");
        }
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