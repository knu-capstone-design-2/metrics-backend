package kr.cs.interdata.consumer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ThresholdRequest {
    private String typeId;
    private String metric;
    private Double value;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp; // 임계값을 넘은 시각

    public ThresholdRequest(String typeId, String metric, Double value, LocalDateTime timestamp) {
        this.typeId = typeId;
        this.metric = metric;
        this.value = value;
        this.timestamp = timestamp;
    }


}
