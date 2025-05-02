package kr.cs.interdata.datacollector;
import com.google.gson.Gson;//자바 객체를 JSON 문자열로 바꿔주는 라이브러리
// 파일을 읽을 때 사용하는 라이브러리들
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

// 로그(프로그램 동작 기록)를 남기기 위한 라이브러리
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.HashMap;
import java.util.Map;

public class ContainerResourceMonitor {
    // 컨테이너 내부에서 리소스 사용량을 수집하는 역할을 하는 클래스

    // Logger: 프로그램 실행 중 발생하는 에러나 정보를 기록하는 도구
    // 로그를 기록하기 위해서 Logger 객체를 클래스 단위로 생성
    //프로그램 실행 중 발생하는 정보나 에러 같은거 기록할 때 사용
    private static final Logger logger = Logger.getLogger(ContainerResourceMonitor.class.getName());

    public static String readFile(String path) {
        //정상적으로 읽었을 때는 파일의 내용을 담은 문자열을 반환하고 읽기 실패하면 null을 반환함.
        // 파일의 내용을 읽어서 문자열로 반환하는 함수

        //입력 -> 읽을 파일의 경로(path)->문자열임
        try {
            //해당 경로 파일을 읽어서 파일 내용을 문자열로 반환
            //파일의 모든 바이트를 읽어서 문자열로 만들고 앞뒤 공백을 제거함.->그래서 trim으로 자름
            return new String(Files.readAllBytes(Paths.get(path))).trim();
        } catch (IOException e) {
            // 파일을 읽을 수 없으면 에러 로그를 남기고 null을 반환함.
            logger.log(Level.SEVERE, "Failed to read file: " + path, e);
            return null;
        }
    }

    public static Long readLongFromFile(String path) {
        //파일 내용이 숫자라면 그 값을 long 타입으로 반환하고 실패하면 null을 반환함.
        // 파일에서 숫자를 읽어오는 함수->long type임!
        // 입력 -> 읽을 파일의 경로(path)->문자열

        String content = readFile(path);//readFile 함수를 이용해 파일 내용을 문자열로 읽어옴
        if (content == null) return null;
        try {
            return Long.parseLong(content);//읽어온 문자열을 long 타입 숫자로 변환함.
        } catch (NumberFormatException e) {
            // 읽기 실패하면 null을 반환함.
            //문자열이 숫자가 아니거나 변환에 실패하면 에러 로그를 남기고 null을 반환함.
            logger.log(Level.SEVERE, "Failed to parse long from file: " + path + " (content: " + content + ")", e);
            return null;
        }
    }
    public static String collectContainerResources() {
        // 실제로 컨테이너 리소스 정보를 수집해서 JSON 문자열로 반환하는 함수
        Map<String, Object> jsonMap = new HashMap<>();

        // type: 이 데이터가 컨테이너에서 왔다는 표시
        jsonMap.put("type", "container");

        // containerId: 컨테이너의 호스트네임(보통 컨테이너 ID와 같음)
        String containerId = "unknown";//예외 발생 시 대비하기 위해서 기본값을 설정
        try {
            //자바의 InetAddress.getLocalHost().getHostName()을 호출하면 기본적으로 컨테이너 ID의 앞 12자리가 반환
            //12자리는 Docker 내에서 유일하지만 100% 고유성을 원하면 64자리 ID 읽는 방법을 사용해야 함.
            containerId = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            //hostname을 얻는 과정에서 오류가 발생하면 에러 로그를 남기고 containerID는 unknown으로 남음
            logger.log(Level.SEVERE, "Failed to get containerId (hostname)", e);
        }
        jsonMap.put("containerId", containerId);//JSON 데이터에 포함 시킴

        //cpu 사용량 측정
        //1초 간격으로 누적 cpu 사용량을 2번 읽고 그 차이를 이욯해 1초 동안 CPU 사용률을 계산함.
        ///sys/fs/cgroup/cpuacct/cpuacct.usage 파일은 컨테이너 혹은 프로세스가 지금까지 사용한 누적 CPU 시간(나노초 단위)를 기록
        //https://velog.io/@hsh_124/cgroup-%EC%9D%84-%ED%86%B5%ED%95%B4-%EC%BB%A8%ED%85%8C%EC%9D%B4%EB%84%88%EC%9D%98-%EB%A6%AC%EC%86%8C%EC%8A%A4-%ED%99%95%EC%9D%B8%ED%95%98%EA%B8%B0
        Long cpuUsageBefore=readLongFromFile("/sys/fs/cgroup/cpuacct/cpuacct.usage");

        try{
            Thread.sleep(1000);//1초동안 현재 스레드 멈춤
        }catch (InterruptedException e){
            //누군가 이 스레드에게 멈춰라고 신호를 보내서(인터럽트) 자고 있던 스레드가 깼을 때
            //자바는 멈춰 신호를 자동으로 잊어버리기 때문에 catch 블록에서 다시 멈춰 신호를 켜줌.
            Thread.currentThread().interrupt();
            //인터럽트가 발생했다는 경고 메시지 남김.
            logger.log(Level.WARNING,"Thread was interruped during CPU usage measurement",e);
        }
        //1초가 지난 후 다시 같은 파일에서 누적 CPU 사용량 읽어옴
        //누적 사용량이기 때문에 이 값은 cpuUsageBefore보다 값이 커져있음
        Long cpuUsageAfter=readLongFromFile("/sys/fs/cgroup/cpuacct/cpuacct.usage");
        double cpuUsagePercent=-1;
        if (cpuUsageBefore != null && cpuUsageAfter != null) {
            //두 값이 모두 null이 아니면 계산을 시작
            long delta = cpuUsageAfter - cpuUsageBefore;//1초 동안 실제로 사용한 CPU 시간
            cpuUsagePercent = (delta / 1_000_000_000.0) * 100;//나노초 단위이기 때문에 초로 변환
            //1초 동안 1코어를 100% 사용했다고 가정할 때, 실제 사용량을 퍼센트로 환산
        }
        jsonMap.put("cpuUsage", cpuUsagePercent);

        // 메모리 정보 수집
        //memory.limit_in_bytes: 컨테이너에 할당된 최대 메모리
        //memory.usage_in_bytes: 현재 사용중인 메모리
        /**
        Long memoryLimit = readLongFromFile("/sys/fs/cgroup/memory/memory.limit_in_bytes");
        if (memoryLimit == null) {
            memoryLimit = readLongFromFile("/sys/fs/cgroup/memory.max");
        }
        **/
        Long memoryUsage = readLongFromFile("/sys/fs/cgroup/memory/memory.usage_in_bytes");
        if (memoryUsage == null) {
            memoryUsage = readLongFromFile("/sys/fs/cgroup/memory.current");
        }
        /**
        Long memoryFree=null;
        if (memoryLimit != null && memoryUsage != null) {
            memoryFree = memoryUsage - memoryLimit;
        }
         **/
        //JSON에 값 넣기
        /**
        if (memoryFree!=null) {
            jsonMap.put("memoryTotalBytes",memoryLimit);
        }else{
            jsonMap.put("memoryTotalBytes",-1);
        }
        **/
        if (memoryUsage!=null){
            jsonMap.put("memoryUsedBytes",memoryUsage);
        }else{
            jsonMap.put("memoryUsedBytes",-1);
        }
        /**
        if (memoryFree!=null){
            jsonMap.put("memoryFreeBytes",memoryFree);
        }else{
            jsonMap.put("memoryFreeBytes",-1);
        }
        **/

        return new Gson().toJson(jsonMap);
    }


}
