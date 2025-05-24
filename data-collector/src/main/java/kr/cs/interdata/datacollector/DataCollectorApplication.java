package kr.cs.interdata.datacollector;

import com.google.gson.Gson;
import kr.cs.interdata.producer.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootApplication(scanBasePackages = "kr.cs.interdata")
public class DataCollectorApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DataCollectorApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        app.run(args);
    }
}

@Component
class DataCollectorRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(DataCollectorRunner.class);
    private final KafkaProducerService kafkaProducerService;

    @Autowired
    public DataCollectorRunner(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @Override
    public void run(String... args) {
        long prevCpuUsageNano = ContainerResourceMonitor.getCpuUsageNano();
        long prevDiskReadBytes = ContainerResourceMonitor.getDiskIO()[0];
        long prevDiskWriteBytes = ContainerResourceMonitor.getDiskIO()[1];
        Map<String, Long[]> prevNetStats = ContainerResourceMonitor.getNetworkStats();

        Gson gson = new Gson();

        while (true) {
            // 자기 자신은 수집과 전송하지 않음
            String excludeSelf = System.getenv("EXCLUDE_SELF");
            if ("true".equalsIgnoreCase(excludeSelf)) {
                logger.info("자기 자신 컨테이너이므로 리소스 수집/전송을 건너뜁니다.");
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
                continue;
            }

            // 누적값만 수집
            long currCpuUsageNano = ContainerResourceMonitor.getCpuUsageNano();
            long currDiskReadBytes = ContainerResourceMonitor.getDiskIO()[0];
            long currDiskWriteBytes = ContainerResourceMonitor.getDiskIO()[1];
            Map<String, Long[]> currNetStats = ContainerResourceMonitor.getNetworkStats();
            Map<String, Object> resourceMap = ContainerResourceMonitor.collectContainerResourceRaw();

            // CPU 사용률(1초 기준, 1코어 100%)
            long deltaCpuNano = currCpuUsageNano - prevCpuUsageNano;
            double cpuUsagePercent = (deltaCpuNano / 1_000_000_000.0) * 100;

            // 디스크 변화량
            long deltaDiskRead = currDiskReadBytes - prevDiskReadBytes;
            long deltaDiskWrite = currDiskWriteBytes - prevDiskWriteBytes;

            // 네트워크 변화량 및 속도
            Map<String, Map<String, Object>> netDelta = new HashMap<>();
            for (String iface : currNetStats.keySet()) {
                Long[] curr = currNetStats.get(iface);
                Long[] prev = prevNetStats.getOrDefault(iface, new Long[]{curr[0], curr[1]});
                long deltaRecv = curr[0] - prev[0];
                long deltaSent = curr[1] - prev[1];

                Map<String, Object> ifaceDelta = new HashMap<>();
                ifaceDelta.put("rxBytesDelta", deltaRecv);
                ifaceDelta.put("txBytesDelta", deltaSent);
                ifaceDelta.put("rxBps", deltaRecv); // 1초마다 반복이므로 deltaRecv가 Bps
                ifaceDelta.put("txBps", deltaSent);
                netDelta.put(iface, ifaceDelta);

                prevNetStats.put(iface, curr);
            }

            // 최종 JSON 생성
            Map<String, Object> resultJson = new LinkedHashMap<>();
            resultJson.put("type", resourceMap.get("type"));
            resultJson.put("containerId", resourceMap.get("containerId"));
            resultJson.put("cpuUsagePercent", cpuUsagePercent);
            resultJson.put("memoryUsedBytes", resourceMap.get("memoryUsedBytes"));
            resultJson.put("diskReadBytesDelta", deltaDiskRead);
            resultJson.put("diskWriteBytesDelta", deltaDiskWrite);
            resultJson.put("networkDelta", netDelta);

            String jsonPayload = gson.toJson(resultJson);

            //System.out.println("=== 컨테이너 리소스 변화량 수집 결과 ===");
            System.out.println(jsonPayload);

            // Kafka로 전송
            kafkaProducerService.routeMessageBasedOnType(jsonPayload);

            // 이전값 갱신
            prevCpuUsageNano = currCpuUsageNano;
            prevDiskReadBytes = currDiskReadBytes;
            prevDiskWriteBytes = currDiskWriteBytes;
            prevNetStats = currNetStats;

            try {
                Thread.sleep(1000); // 1초마다 반복
            } catch (InterruptedException e) {
                System.out.println("스레드가 인터럽트되었습니다.");
            }
        }
    }
}
