package kr.cs.interdata.consumer.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ThresholdRequest {
    private String type;
    private String metric;
    private Double value;
    private LocalDateTime detected_at;

    public ThresholdRequest(String type, String metric, Double value, LocalDateTime detected_at) {
        this.type = type;
        this.metric = metric;
        this.value = value;
        this.detected_at = detected_at;
    }


}
