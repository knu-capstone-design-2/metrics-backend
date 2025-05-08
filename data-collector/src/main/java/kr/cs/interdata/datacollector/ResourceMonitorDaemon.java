package kr.cs.interdata.datacollector;

import com.google.gson.Gson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResourceMonitorDaemon {
    public static void main(String[] args) throws Exception {
        LocalHostResourceMonitor monitor = new LocalHostResourceMonitor();

        // 이전 디스크 I/O 값 저장
        long prevDiskReadBytes = 0;
        long prevDiskWriteBytes = 0;

        // 이전 네트워크 인터페이스별 값 저장
        Map<String, Long> prevNetRecv = new HashMap<>();
        Map<String, Long> prevNetSent = new HashMap<>();

        ObjectMapper prettyMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        while (true) {
            String jsonStr = monitor.getResourcesAsJson();
            Map<String, Object> resourceMap = new Gson().fromJson(jsonStr, Map.class);

            // 1. 디스크 변화량 계산
            long currDiskReadBytes = ((Number) resourceMap.get("diskReadBytes")).longValue();
            long currDiskWriteBytes = ((Number) resourceMap.get("diskWriteBytes")).longValue();
            long deltaDiskRead = currDiskReadBytes - prevDiskReadBytes;
            long deltaDiskWrite = currDiskWriteBytes - prevDiskWriteBytes;

            // 2. 네트워크 변화량 계산
            Map<String, Object> netInfo = (Map<String, Object>) resourceMap.get("network");
            Map<String, Map<String, Object>> netDelta = new HashMap<>();
            for (String iface : netInfo.keySet()) {
                Map<String, Object> ifaceInfo = (Map<String, Object>) netInfo.get(iface);
                long currRecv = ((Number) ifaceInfo.get("bytesReceived")).longValue();
                long currSent = ((Number) ifaceInfo.get("bytesSent")).longValue();
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

            // 3. 필요 없는 누적값(디스크/네트워크) 삭제
            resourceMap.remove("diskReadBytes");
            resourceMap.remove("diskWriteBytes");
            resourceMap.remove("network");

            // 4. 변화량만 추가
            resourceMap.put("diskReadBytesDelta", deltaDiskRead);
            resourceMap.put("diskWriteBytesDelta", deltaDiskWrite);
            resourceMap.put("networkDelta", netDelta);

            // 5. 예쁘게 출력
            String prettyJson = prettyMapper.writeValueAsString(resourceMap);
            //System.out.println("=== 로컬 호스트 리소스 수집 결과 ===");
            System.out.println(prettyJson);

            // 6. 이전값 갱신
            prevDiskReadBytes = currDiskReadBytes;
            prevDiskWriteBytes = currDiskWriteBytes;

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                // 인터럽트가 발생하면 로그만 남기고 계속 반복
                System.out.println("Interrupted, but monitoring continues.");
            }
        }
    }
}

       /**
        //무한 루프 : 5초마다 리소스 데이터 수집 및 출력
        while (true) {
            // 현재 리소스 전체 누적값(JSON 문자열) 파싱
            String jsonStr = monitor.getResourcesAsJson();
            Map<String, Object> resourceMap = new Gson().fromJson(jsonStr, Map.class);

            // 2. 디스크 변화량 계산
            long currDiskReadBytes = ((Number) resourceMap.get("diskReadBytes")).longValue();
            long currDiskWriteBytes = ((Number) resourceMap.get("diskWriteBytes")).longValue();
            long deltaDiskRead = currDiskReadBytes - prevDiskReadBytes;
            long deltaDiskWrite = currDiskWriteBytes - prevDiskWriteBytes;

            // 3. 네트워크 변화량 계산
            Map<String, Object> netInfo = (Map<String, Object>) resourceMap.get("network");
            Map<String, Map<String, Object>> netDelta = new HashMap<>();
            for (String iface : netInfo.keySet()) {
                Map<String, Object> ifaceInfo = (Map<String, Object>) netInfo.get(iface);
                long currRecv = ((Number) ifaceInfo.get("bytesReceived")).longValue();
                long currSent = ((Number) ifaceInfo.get("bytesSent")).longValue();
                long prevRecv = prevNetRecv.getOrDefault(iface, currRecv);
                long prevSent = prevNetSent.getOrDefault(iface, currSent);

                long deltaRecv = currRecv - prevRecv;
                long deltaSent = currSent - prevSent;

                Map<String, Object> ifaceDelta = new HashMap<>();
                ifaceDelta.put("rxBytesDelta", deltaRecv);
                ifaceDelta.put("txBytesDelta", deltaSent);
                netDelta.put(iface, ifaceDelta);

                prevNetRecv.put(iface, currRecv);
                prevNetSent.put(iface, currSent);
            }

            // 4. 최종 JSON에 변화량 정보 추가
            resourceMap.put("diskReadBytesDelta", deltaDiskRead);
            resourceMap.put("diskWriteBytesDelta", deltaDiskWrite);
            resourceMap.put("networkDelta", netDelta);

            // 5. 예쁘게 출력(Pretty Print)
            String prettyJson = prettyMapper.writeValueAsString(resourceMap);
            System.out.println("=== 로컬 호스트 리소스 수집 결과 ===");
            System.out.println(prettyJson);

            // 6. 이전값 갱신
            prevDiskReadBytes = currDiskReadBytes;
            prevDiskWriteBytes = currDiskWriteBytes;

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("스레드가 인터럽트되었습니다.");
                break;
            }
        }
    }
}
**/