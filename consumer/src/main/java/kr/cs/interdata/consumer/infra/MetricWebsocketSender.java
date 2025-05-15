package kr.cs.interdata.consumer.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class MetricWebsocketSender {

    // JSON 변환을 위한 ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(MetricWebsocketSender.class);

    private final WebClient webClient;
    private final MetricWebsocketHandler metricWebsocketHandler;

    @Autowired
    public MetricWebsocketSender(WebClient webClient, MetricWebsocketHandler metricWebsocketHandler) {
        this.webClient = webClient;
        this.metricWebsocketHandler = metricWebsocketHandler;
    }

    public void handleMessage(String machineId, String type, Object metricData) {
        try {
            // API 호출로 Target ID 가져오기
            String requestUrl = "/api/inventory/" + machineId + "/" + type;

            Mono<String> responseMono = webClient
                    .get()
                    .uri(requestUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(e -> logger.error("Failed to call API: {}", e.getMessage()));

            // 비동기 응답 처리
            responseMono.subscribe(targetId -> {
                if (targetId == null || targetId.isEmpty()) {
                    logger.warn("Unique ID not found for Machine ID: {}", machineId);
                    return;
                }

                try {
                    // metricData를 Map으로 변환
                    Map<String, Object> dataMap = objectMapper.convertValue(
                            metricData,
                            new TypeReference<Map<String, Object>>() {}
                    );

                    // hostId/containerId 키의 값을 targetId로 변경
                    if (dataMap.containsKey("hostId")) {
                        dataMap.put("hostId", targetId);
                    } else if (dataMap.containsKey("containerId")) {
                        dataMap.put("containerId", targetId);
                    } else {
                        logger.warn("hostId/containerId not found in the metric data");
                    }

                    // JSON 문자열로 변환
                    String jsonMessage = objectMapper.writeValueAsString(dataMap);

                    // WebSocket으로 전송
                    metricWebsocketHandler.sendMetricMessage(jsonMessage);

                } catch (Exception e) {
                    logger.error("Failed to process message for Machine ID: {}, Type: {}. Error: {}", machineId, type, e.getMessage());
                }
            });

        } catch (Exception e) {
            logger.error("Failed to process message for Machine ID: {}, Type: {}. Error: {}", machineId, type, e.getMessage());
        }
    }

}
