package kr.cs.interdata.datacollector;

import com.google.gson.Gson;
import kr.cs.interdata.producer.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
@SpringBootApplication(scanBasePackages = "kr.cs.interdata")
public class DataCollectorApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataCollectorRunner.class, args);
    }
}
**/
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

    private final KafkaProducerService kafkaProducerService;

    @Autowired
    public DataCollectorRunner(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @Override
    public void run(String... args) {
        long prevDiskReadBytes = 0;
        long prevDiskWriteBytes = 0;
        Map<String, Long> prevNetRecv = new HashMap<>();
        Map<String, Long> prevNetSent = new HashMap<>();
        Gson gson = new Gson();

        while (true) {
            String json = ContainerResourceMonitor.collectContainerResources();
            Map<String, Object> jsonMap = gson.fromJson(json, Map.class);

            // 디스크 변화량 계산
            long currDiskReadBytes = ((Number) jsonMap.get("diskReadBytes")).longValue();
            long currDiskWriteBytes = ((Number) jsonMap.get("diskWriteBytes")).longValue();
            long deltaDiskRead = currDiskReadBytes - prevDiskReadBytes;
            long deltaDiskWrite = currDiskWriteBytes - prevDiskWriteBytes;

            // 네트워크 변화량 계산
            Map<String, Object> network = (Map<String, Object>) jsonMap.get("network");
            Map<String, Map<String, Object>> netDelta = new HashMap<>();
            for (String iface : network.keySet()) {
                Map<String, Object> ifaceMap = (Map<String, Object>) network.get(iface);
                long currRecv = ((Number) ifaceMap.get("bytesReceived")).longValue();
                long currSent = ((Number) ifaceMap.get("bytesSent")).longValue();
                long prevRecv = prevNetRecv.getOrDefault(iface, currRecv);
                long prevSent = prevNetSent.getOrDefault(iface, currSent);
                long deltaRecv = currRecv - prevRecv;
                long deltaSent = currSent - prevSent;

                // 네트워크 속도(Bps, 초당 바이트)
                long rxBps = deltaRecv / 5;
                long txBps = deltaSent / 5;

                Map<String, Object> ifaceDelta = new HashMap<>();
                ifaceDelta.put("rxBytesDelta", deltaRecv);
                ifaceDelta.put("txBytesDelta", deltaSent);
                ifaceDelta.put("rxBps", rxBps);
                ifaceDelta.put("txBps", txBps);
                netDelta.put(iface, ifaceDelta);

                prevNetRecv.put(iface, currRecv);
                prevNetSent.put(iface, currSent);
            }

            // 새로운 JSON에 변화량만 남기고, 누적값/불필요한 값은 제거
            Map<String, Object> resultJson = new LinkedHashMap<>();
            resultJson.put("type", jsonMap.get("type"));
            resultJson.put("containerId", jsonMap.get("containerId"));
            resultJson.put("cpuUsage", jsonMap.get("cpuUsage"));
            resultJson.put("memoryUsedBytes", jsonMap.get("memoryUsedBytes"));
            resultJson.put("diskReadBytesDelta", deltaDiskRead);
            resultJson.put("diskWriteBytesDelta", deltaDiskWrite);
            resultJson.put("networkDelta", netDelta);

            String jsonPayload = gson.toJson(resultJson);

            System.out.println("=== 컨테이너 리소스 변화량 수집 결과 ===");
            System.out.println(jsonPayload);

            // Kafka로 전송
            kafkaProducerService.routeMessageBasedOnType(jsonPayload);

            // 이전값 갱신
            prevDiskReadBytes = currDiskReadBytes;
            prevDiskWriteBytes = currDiskWriteBytes;

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("스레드가 인터럽트되었습니다.");
            }
        }
    }
}
/**
package kr.cs.interdata.datacollector;
import java.io.File;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import com.google.gson.Gson;
@SpringBootApplication
public class DataCollectorApplication {
    public static void main(String[] args) {
        // 이전 디스크/네트워크 값 저장용 변수
        long prevDiskReadBytes = 0;
        long prevDiskWriteBytes = 0;
        Map<String, Long> prevNetRecv = new HashMap<>();
        Map<String, Long> prevNetSent = new HashMap<>();

        Gson gson = new Gson();

        while (true) {
            String json = ContainerResourceMonitor.collectContainerResources();
            Map<String, Object> jsonMap = gson.fromJson(json, Map.class);

            // 디스크 변화량 계산
            long currDiskReadBytes = ((Number) jsonMap.get("diskReadBytes")).longValue();
            long currDiskWriteBytes = ((Number) jsonMap.get("diskWriteBytes")).longValue();
            long deltaDiskRead = currDiskReadBytes - prevDiskReadBytes;
            long deltaDiskWrite = currDiskWriteBytes - prevDiskWriteBytes;

            // 네트워크 변화량 계산
            Map<String, Object> network = (Map<String, Object>) jsonMap.get("network");
            Map<String, Map<String, Object>> netDelta = new HashMap<>();
            for (String iface : network.keySet()) {
                Map<String, Object> ifaceMap = (Map<String, Object>) network.get(iface);
                long currRecv = ((Number) ifaceMap.get("bytesReceived")).longValue();
                long currSent = ((Number) ifaceMap.get("bytesSent")).longValue();
                long prevRecv = prevNetRecv.getOrDefault(iface, currRecv);
                long prevSent = prevNetSent.getOrDefault(iface, currSent);
                long deltaRecv = currRecv - prevRecv;
                long deltaSent = currSent - prevSent;

                // 네트워크 속도(Bps, 초당 바이트)
                long rxBps = deltaRecv / 5;
                long txBps = deltaSent / 5;

                Map<String, Object> ifaceDelta = new HashMap<>();
                ifaceDelta.put("rxBytesDelta", deltaRecv);
                ifaceDelta.put("txBytesDelta", deltaSent);
                ifaceDelta.put("rxBps", rxBps); // 초당 받은 바이트
                ifaceDelta.put("txBps", txBps); // 초당 보낸 바이트
                netDelta.put(iface, ifaceDelta);

                prevNetRecv.put(iface, currRecv);
                prevNetSent.put(iface, currSent);
            }

            // 새로운 JSON에 변화량만 남기고, 누적값/불필요한 값은 제거
            Map<String, Object> resultJson = new LinkedHashMap<>();
            resultJson.put("type", jsonMap.get("type"));
            resultJson.put("containerId", jsonMap.get("containerId"));
            resultJson.put("cpuUsage", jsonMap.get("cpuUsage"));
            resultJson.put("memoryUsedBytes", jsonMap.get("memoryUsedBytes"));
            resultJson.put("diskReadBytesDelta", deltaDiskRead);
            resultJson.put("diskWriteBytesDelta", deltaDiskWrite);
            resultJson.put("networkDelta", netDelta);

            System.out.println("=== 컨테이너 리소스 변화량 수집 결과 ===");
            System.out.println(gson.toJson(resultJson));

            // 이전값 갱신
            prevDiskReadBytes = currDiskReadBytes;
            prevDiskWriteBytes = currDiskWriteBytes;

            try {
                Thread.sleep(5000); // 5초마다 반복
            } catch (InterruptedException e) {
                //Thread.currentThread().interrupt();
                System.out.println("스레드가 인터럽트되었습니다.");
                //break;
            }
        }
    }
}
        /**
            while (true) {
                String json = ContainerResourceMonitor.collectContainerResources();
                System.out.println("=== 컨테이너 리소스 수집 결과 ===");
                System.out.println(json);

                try {
                    Thread.sleep(5000); // 5초마다 반복
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("스레드가 인터럽트되었습니다.");
                    break;
                }
            }
**/
