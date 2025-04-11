package kr.cs.interdata.consumer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.cs.interdata.consumer.entity.MetricsByType;
import kr.cs.interdata.consumer.entity.TargetType;
import kr.cs.interdata.consumer.repository.MetricsByTypeRepository;
import kr.cs.interdata.consumer.repository.TargetTypeRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@Service
public class MonitoringDefinitionService {

    private final Logger logger = LoggerFactory.getLogger(MonitoringDefinitionService.class);
    private final TargetTypeRepository targetTypeRepository;
    private final MetricsByTypeRepository metricsByTypeRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public MonitoringDefinitionService(
            TargetTypeRepository targetTypeRepository,
            MetricsByTypeRepository metricsByTypeRepository,
            ObjectMapper objectMapper) {
        this.targetTypeRepository = targetTypeRepository;
        this.metricsByTypeRepository = metricsByTypeRepository;
        this.objectMapper = objectMapper;
    }

    // 타입 등록 - targetType
    public void registerType(String type) {
        TargetType targetType = null;
        targetType.setType(type);
        targetTypeRepository.save(targetType);

        logger.info("Register type "+type+" successfully.");
    }

    public void saveMetric(String metricName, String unit, Double threshold){
        MetricsByType metric = null;
        metric.setMetric_name(metricName);
        metric.setUnit(unit);
        metric.setThreshold_value(threshold);
        metricsByTypeRepository.save(metric);

        logger.info("Save new metric : " + metricName + "(" + unit + ")");
    }

    // 새로 받아올 메트릭 저장 - metricsbytype
    // 메트릭의 임계값 수정 - metricsbytype
    // 필요없어진 메트릭 삭제 - metricsbytype

}
