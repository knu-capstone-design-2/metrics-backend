/*
package kr.cs.interdata.consumer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.cs.interdata.consumer.entity.ResourceMetrics;
import kr.cs.interdata.consumer.repository.ResourceMetricsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class RepositoryService {

    private final ResourceMetricsRepository resourceMetricsRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public RepositoryService(ResourceMetricsRepository resourceMetricsRepository, ObjectMapper objectMapper) {
        this.resourceMetricsRepository = resourceMetricsRepository;
        this.objectMapper = objectMapper;
    }

    */
/**
     *  - JSON 데이터를 객체로 변환 후, DB에 저장하는 메소드
     *
     * @param message       // 유효성 검사를 한 json형태의 kafka value의 metrics
     * @param sourceType    // 'host' 혹은 'container'
     * @param containerId   // host라면 'NULL', container라면 'containerId'
     *//*

    public void saveMetrics(String message, String sourceType, String containerId) {
        try {
            ResourceMetrics metrics = objectMapper.readValue(message, ResourceMetrics.class);
            metrics.setSourceType(sourceType);
            metrics.setContainerId(containerId);

            resourceMetricsRepository.save(metrics);

            System.out.println("Metrics saved to DB: " + metrics.toString());

        } catch (Exception e) {
            System.err.println("Error parsing message: " + e.getMessage());
        }
    }


}
*/
