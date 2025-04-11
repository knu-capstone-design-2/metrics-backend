package kr.cs.interdata.consumer.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ResourceMetrics")
@Getter
@Setter
public class ResourceMetrics {

    //kafka 적재 전 id를 부여할건지, db적재 직전 id를 부여할건지 논의 중
    @Id  // set private key
    private String id;

    private String sourceType;  // "host" or "container"
    private String containerId; // container; id, host; NULL

    // metrics
    // metrics들은 host와 container가 받아오는 값이 다르다
    // -> 이를 다 받아오기 위해 host와 container를 나눌건지
    // -> 같은 값을 처리할건지 논의 중
    // -> 이에 따라 변수 값과 더불어 db table의 key 구성이 달라질 예정
    private double cpuUsage;
    private double memoryUsage;
    private double totalMemory;
    private double diskUsage;
    private double totalDisk;

    // time stamp
    private LocalDateTime timeStamp;

    // 생성자
    public void KafkaResourceMetrics() {
        this.cpuUsage = 0;
        this.memoryUsage = 0;
        this.totalMemory = 0;
        this.diskUsage = 0;
        this.totalDisk = 0;
        this.timeStamp = LocalDateTime.now();
    }

    public String toString() {
        return "source type : " + sourceType +
                "\n[cpuUsage : " + cpuUsage + ",\n memoryUsage : " + memoryUsage +
                ", totalMemory : " + totalMemory +
                ",\n diskUsage : " + diskUsage +
                ", totalDisk : " + totalDisk +
                ",\n timeStamp : " + timeStamp + "]";
    }

}

/*
 *  mock data ->value값에 넣을 데이터
 *
// 완벽한 json 형태
{cpuUsage="50.2",memoryUsage="49.6",totalMemory="100.0",diskUsage="69.6",totalDisk="100.0",timeStamp="2025-03-27T11:37:21Z"}
// networkUsage="194.11.14.133"

// 만약 network usage? 쓴다면,
// network usage는 table을 나눠야 함. ResourceMetrics <- NetworkUsage 이렇게 private key값은 network 주소로.
*/