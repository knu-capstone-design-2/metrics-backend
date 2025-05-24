package kr.cs.interdata.datacollector;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ContainerResourceMonitor {
    private static final Logger logger = Logger.getLogger(ContainerResourceMonitor.class.getName());

    public static String readFile(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path))).trim();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read file: " + path, e);
            return null;
        }
    }

    public static Long readLongFromFile(String path) {
        String content = readFile(path);
        if (content == null) return null;
        try {
            return Long.parseLong(content);
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE, "Failed to parse long from file: " + path + " (content: " + content + ")", e);
            return null;
        }
    }

    // 네트워크 인터페이스별 누적 바이트 반환
    public static Map<String, Long[]> getNetworkStats() {
        Map<String, Long[]> networkStats = new HashMap<>();
        String netDev = readFile("/proc/net/dev");
        if (netDev == null) return networkStats;
        String[] lines = netDev.split("\n");
        for (int i = 2; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(":");
            if (parts.length < 2) continue;
            String iface = parts[0].trim();
            String[] data = parts[1].trim().split("\\s+");
            if (data.length < 16) continue;
            try {
                long bytesReceived = Long.parseLong(data[0]);
                long bytesSent = Long.parseLong(data[8]);
                networkStats.put(iface, new Long[]{bytesReceived, bytesSent});
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Failed to parse network stats for interface: " + iface, e);
            }
        }
        return networkStats;
    }

    // CPU 누적 사용량(나노초) 반환 (cgroup v1/v2 모두 지원)
    public static Long getCpuUsageNano() {
        String v1Path = "/sys/fs/cgroup/cpuacct/cpuacct.usage";
        if (Files.exists(Paths.get(v1Path))) {
            try {
                String content = Files.readString(Paths.get(v1Path)).trim();
                return Long.parseLong(content);
            } catch (IOException | NumberFormatException e) {
                logger.log(Level.WARNING, "Failed to read v1 cpuacct.usage", e);
            }
        } else {
            String v2Path = "/sys/fs/cgroup/cpu.stat";
            if (Files.exists(Paths.get(v2Path))) {
                try {
                    for (String line : Files.readAllLines(Paths.get(v2Path))) {
                        if (line.startsWith("usage_usec")) {
                            String[] parts = line.split("\\s+");
                            return Long.parseLong(parts[1]) * 1000L; // 마이크로초 → 나노초
                        }
                    }
                } catch (IOException | NumberFormatException e) {
                    logger.log(Level.WARNING, "Failed to read v2 cpu.stat", e);
                }
            }
        }
        return null;
    }

    // 메모리 사용량(누적)
    public static Long getMemoryUsage() {
        Long memoryUsage = readLongFromFile("/sys/fs/cgroup/memory/memory.usage_in_bytes");
        if (memoryUsage == null) {
            memoryUsage = readLongFromFile("/sys/fs/cgroup/memory.current");
        }
        return memoryUsage;
    }

    // 디스크 I/O (누적)
    public static long[] getDiskIO() {
        long diskReadBytes = 0;
        long diskWriteBytes = 0;
        String blkioData = readFile("/sys/fs/cgroup/blkio/io_service_bytes_recursive");
        if (blkioData == null) {
            blkioData = readFile("/sys/fs/cgroup/io.stat");
            if (blkioData != null) {
                String[] lines = blkioData.split("\n");
                for (String line : lines) {
                    String[] parts = line.trim().split("\\s+");
                    for (String part : parts) {
                        if (part.startsWith("rbytes=")) {
                            try {
                                diskReadBytes += Long.parseLong(part.substring(7));
                            } catch (NumberFormatException e) {
                                logger.log(Level.WARNING, "Failed to parse rbytes: " + part, e);
                            }
                        } else if (part.startsWith("wbytes=")) {
                            try {
                                diskWriteBytes += Long.parseLong(part.substring(7));
                            } catch (NumberFormatException e) {
                                logger.log(Level.WARNING, "Failed to parse wbytes: " + part, e);
                            }
                        }
                    }
                }
            }
        } else {
            String[] lines = blkioData.split("\n");
            for (String line : lines) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 3) {
                    String op = parts[1];
                    try {
                        long value = Long.parseLong(parts[2]);
                        if ("Read".equalsIgnoreCase(op)) {
                            diskReadBytes += value;
                        } else if ("Write".equalsIgnoreCase(op)) {
                            diskWriteBytes += value;
                        }
                    } catch (NumberFormatException e) {
                        logger.log(Level.WARNING, "Failed to parse blkio value: " + line, e);
                    }
                }
            }
        }
        return new long[]{diskReadBytes, diskWriteBytes};
    }

    // 리소스 누적값을 모두 반환 (변화량 계산은 외부 while문에서)
    public static Map<String, Object> collectContainerResourceRaw() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "container");
        try {
            map.put("containerId", java.net.InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to get containerId (hostname)", e);
            map.put("containerId", "unknown");
        }
        map.put("cpuUsageNano", getCpuUsageNano());
        map.put("memoryUsedBytes", getMemoryUsage());
        long[] diskIO = getDiskIO();
        map.put("diskReadBytes", diskIO[0]);
        map.put("diskWriteBytes", diskIO[1]);
        // 네트워크: iface별 [받은 바이트, 보낸 바이트]
        map.put("network", getNetworkStats());
        return map;
    }
}
