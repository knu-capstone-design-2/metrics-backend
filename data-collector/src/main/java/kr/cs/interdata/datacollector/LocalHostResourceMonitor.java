package kr.cs.interdata.datacollector;

import kr.cs.interdata.datacollector.LocalHostNetworkMonitor;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.hardware.NetworkIF;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class LocalHostResourceMonitor {
    private final SystemInfo systemInfo;// 시스템 전체 정보를 제공하는 객체
    private final CentralProcessor processor;//cpu관련
    private final GlobalMemory memory;//메모리 관련 정보
    private final OperatingSystem os;//운영체제 정보
    private long[] prevTicks;//이전 cpu 사용 상태를 저장->cpu 사용률 계산에 씀
    private static final String HOST_ID_FILE = getDefaultHostIdPath();//호스트 ID를 저장하는 파일

    private static String getDefaultHostIdPath() {
        //os에 따라 호스트 고유 id 저장할 파일 경로를 다르게 설정하기 위해 만들어놓긴 함.
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {//window
            return System.getProperty("java.io.tmpdir") + "host-unique-id.txt"; // 예: C:\Users\xxx\AppData\Local\Temp\
        } else {
            return "/tmp/host-unique-id.txt";
        }
    }
    public LocalHostResourceMonitor() {
        //생성자
        //시스템의 하드웨어 정보들 초기화
        systemInfo = new SystemInfo();
        processor = systemInfo.getHardware().getProcessor();
        memory = systemInfo.getHardware().getMemory();
        os = systemInfo.getOperatingSystem();
        prevTicks = processor.getSystemCpuLoadTicks();//초기 cpu 상태 저장
        //this.networkIFS = systemInfo.getHardware().getNetworkIFs();
    }

    public double getCpuUsagePercent() {
        //cpu 사용률 계산
        long[] currentTicks = processor.getSystemCpuLoadTicks();
        double load = processor.getSystemCpuLoadBetweenTicks(prevTicks);
        prevTicks = currentTicks;//다음 계산하기 위해서 현재 상태 저장해야함.
        return load * 100.0;
    }

    public long getTotalMemoryBytes() {
        //총 메모리 크기(byte)
        return memory.getTotal();
    }

    public long getAvailableMemoryBytes() {
        //남은? 사용 가능한 메모리 크기(byte)
        return memory.getAvailable();
    }

    public long getUsedMemoryBytes() {
        //사용 중인 메모리 크기
        return getTotalMemoryBytes() - getAvailableMemoryBytes();//총 메모리 - 사용 가능한 메모리
    }

    public long getTotalDiskBytes() {
        //총 디스크 용량 -> 모든 디스크 합침(byte)
        long total = 0;
        for (OSFileStore fs : os.getFileSystem().getFileStores()) {
            total += fs.getTotalSpace();
        }
        return total;
    }

    public long getFreeDiskBytes() {
        //사용 가능한 디스크 용량
        long free = 0;
        for (OSFileStore fs : os.getFileSystem().getFileStores()) {
            free += fs.getUsableSpace();
        }
        return free;
    }

    public long getUsedDiskBytes() {
        //사용중인 디스크 용량
        return getTotalDiskBytes() - getFreeDiskBytes();//총 용량 - 남은 용량
    }

    public long getDiskReadCount() {
        //disk 읽은 수 -> 나중에 필요하면 써야지..ㅎㅎㅎ일단 들고와봄
        long totalReads = 0;
        List<HWDiskStore> diskStores = systemInfo.getHardware().getDiskStores();
        for (HWDiskStore disk : diskStores) {
            disk.updateAttributes();
            totalReads += disk.getReads();
        }
        return totalReads;
    }
    public long getDiskReadBytes() {
        // 디스크에서 읽은 총 바이트 수
        long totalReadBytes = 0;
        List<HWDiskStore> diskStores = systemInfo.getHardware().getDiskStores();
        for (HWDiskStore disk : diskStores) {
            disk.updateAttributes(); // 디스크 상태 최신화
            totalReadBytes += disk.getReadBytes(); // 읽은 바이트 수 누적
        }
        return totalReadBytes;
    }

    public long getDiskWriteBytes() {
        // 디스크에 쓴 총 바이트 수
        long totalWriteBytes = 0;
        List<HWDiskStore> diskStores = systemInfo.getHardware().getDiskStores();
        for (HWDiskStore disk : diskStores) {
            disk.updateAttributes(); // 디스크 상태 최신화
            totalWriteBytes += disk.getWriteBytes(); // 쓴 바이트 수 누적
        }
        return totalWriteBytes;
    }
    public long getDiskWriteCount() {
        //disk 쓴 수 -> 나중에 필요하면 써야지..ㅎㅎㅎ일단 들고와봄
        long totalWrites = 0;
        List<HWDiskStore> diskStores = systemInfo.getHardware().getDiskStores();
        for (HWDiskStore disk : diskStores) {
            disk.updateAttributes();
            totalWrites += disk.getWrites();
        }
        return totalWrites;
    }

    private String getOrCreateHostId() {//이 부분 수정해야함.
        //로컬 파일에 저장된 고유 호스트 ID를 읽거나, 없으면 새로 생성
        //뭔가 이렇게 하면 안될거 같아서 다른 방법 찾아봐야할거 같은..
        //일단 생각해보기
        try {
            Path path = Paths.get(HOST_ID_FILE);
            if (Files.exists(path)) {
                return Files.readString(path).trim();
            } else {
                String uuid = UUID.randomUUID().toString();
                Files.writeString(path, uuid, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                return uuid;
            }
        } catch (IOException e) {
            return UUID.randomUUID().toString(); // fallback
        }
    }

    public String getResourcesAsJson() {
        //모든 resource 정보를 json 형식으로 반환
        LocalHostNetworkMonitor networkMonitor = new LocalHostNetworkMonitor();
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put("type","localhost");//타입 지정

        jsonMap.put("hostId", getOrCreateHostId());//고유 id
        jsonMap.put("cpuUsagePercent", getCpuUsagePercent());
        jsonMap.put("memoryTotalBytes", getTotalMemoryBytes());
        jsonMap.put("memoryUsedBytes", getUsedMemoryBytes());
        jsonMap.put("memoryFreeBytes", getAvailableMemoryBytes());
        jsonMap.put("diskTotalBytes", getTotalDiskBytes());
        jsonMap.put("diskUsedBytes", getUsedDiskBytes());
        jsonMap.put("diskFreeBytes", getFreeDiskBytes());
        jsonMap.put("diskReadBytes", getDiskReadBytes());
        jsonMap.put("diskWriteBytes", getDiskWriteBytes());
        //jsonMap.put("diskReads", getDiskReadCount());
        //jsonMap.put("diskWrites", getDiskWriteCount());
        jsonMap.put("network", networkMonitor.getNetworkInfoJson());

        return new Gson().toJson(jsonMap);//json 문자열로 변환
    }
}

/**package kr.cs.interdata.datacollector;
import kr.cs.interdata.datacollector.LocalHostNetworkMonitor;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import oshi.hardware.NetworkIF;//네트워크 인터페이스 담음

public class LocalHostResourceMonitor {
    //host 머신의 cpu, 메모리, 디스크를 모니터링하고 JSON 형태로 데이터를 반환
    //OSHI 라이브러리를 통해서 시스템 정보를 가져옴
    //final은 변수에 한번만 값 할당 가능하게 하는거임,리스트 내부는 수정할 수 있음.
    private final SystemInfo systemInfo;//시스템 전반의 정보를 제공
    private final CentralProcessor processor;//cpu 정보를 제공
    private final GlobalMemory memory;//메모리 정보
    private final OperatingSystem os;//os 와 파일시스템
    private long[] prevTicks;//CPU 사용률 측정하기 전에 Ticks 저장
    private final List<NetworkIF> networkIFS;//네트워크 인터페이스 리스트


    public LocalHostResourceMonitor() {
        //시스템 정보 초기화
        systemInfo = new SystemInfo();
        processor = systemInfo.getHardware().getProcessor();
        memory = systemInfo.getHardware().getMemory();
        os = systemInfo.getOperatingSystem();
        prevTicks=processor.getSystemCpuLoadTicks();//초기 cpu ticks 값 저장
        //oshi 라이브러리에서 시스템의 모든 네트워크 인터페이스 목록을 한번만 가져와서 저장함.
        SystemInfo si= new SystemInfo();
        this.networkIFS=si.getHardware().getNetworkIFs();
    }
    public double getCpuUsagePercent() {
        //cpu 사용률 반환 (%단위로 나옴)
        //이전 tick과 현재 tick간 차이를 이용해서 cpu 사용률을 계산함.
        long[] currentTicks = processor.getSystemCpuLoadTicks();//현재 tick
        double load = processor.getSystemCpuLoadBetweenTicks(prevTicks)*100;//이전
        prevTicks=currentTicks;//이전 tick 업데이트
        return load * 100.0;//백분율로
    }
    public long getTotalMemoryBytes() {
        //전체 메모리 크기(Byte)
        return memory.getTotal();
    }
    public long getAvailableMemoryBytes() {
        //사용 가능한 메모리 크기(Byte)
        return memory.getAvailable();
    }
    public long getUsedMemoryBytes() {
        //사용 중인 메모리 크기(byte)
        return getTotalMemoryBytes() - getAvailableMemoryBytes();//전체 - 사용 가능
    }
    public long getTotalDiskBytes() {
        //전체 디스크 크기(byte)
        //여러 파일 시스템 합
        long total = 0;
        for (OSFileStore fs : os.getFileSystem().getFileStores()) {
            total += fs.getTotalSpace();//각 파일시스템의 총 공간
        }
        return total;
    }
    public long getFreeDiskBytes() {
        //사용 가능한 디스크 크기(byte)
        long free = 0;
        for (OSFileStore fs : os.getFileSystem().getFileStores()) {
            free += fs.getUsableSpace();//각 파일시스템의 사용 가능 공간
        }
        return free;
    }
    public long getUsedDiskBytes() {
        //사용중인 디스크 크기(byte) = 전체 - 사용 가능
        return getTotalDiskBytes() - getFreeDiskBytes();
    }
    public long getDiskReadCount() {
        //디스크 총 읽기 횟수
        //모든 디스크의 읽기 횟수 총합
        long totalReads = 0;
        List<HWDiskStore> diskStores = systemInfo.getHardware().getDiskStores();
        for (HWDiskStore disk : diskStores) {
            disk.updateAttributes(); // 디스크 상태 최신화
            totalReads += disk.getReads();//읽기 횟수 누적
        }
        return totalReads;
    }
    public long getDiskWriteCount() {
        //디스크 총 쓰기 횟수
        long totalWrites = 0;
        List<HWDiskStore> diskStores = systemInfo.getHardware().getDiskStores();
        for (HWDiskStore disk : diskStores) {
            disk.updateAttributes(); // 디스크 상태 최신화
            totalWrites += disk.getWrites();//쓰기 횟수 누적
        }
        return totalWrites;
    }

    public String getResourcesAsJson() {
        LocalHostNetworkMonitor networkMonitor = new LocalHostNetworkMonitor();
        //전체 리소스 정보를 JSON 문자열로 반환
        Map<String, Object> jsonMap = new HashMap<>();//각 리소스 정보를 Map 형태로 구성
        jsonMap.put("cpuUsagePercent", getCpuUsagePercent());
        jsonMap.put("memoryTotalBytes", getTotalMemoryBytes());
        jsonMap.put("memoryUsedBytes", getUsedMemoryBytes());
        jsonMap.put("memoryFreeBytes", getAvailableMemoryBytes());
        jsonMap.put("diskTotalBytes", getTotalDiskBytes());
        jsonMap.put("diskUsedBytes", getUsedDiskBytes());
        jsonMap.put("diskFreeBytes", getFreeDiskBytes());
        jsonMap.put("diskReads", getDiskReadCount());
        jsonMap.put("diskWrites", getDiskWriteCount());
        // 네트워크 정보 포함 (아래 getNetworkInfoJson 활용)
        jsonMap.put("network",networkMonitor.getNetworkInfoJson());

        //Gson을 이용해 Map을 JSON 문자열로 변환
        return new Gson().toJson(jsonMap);
    }





}
**/