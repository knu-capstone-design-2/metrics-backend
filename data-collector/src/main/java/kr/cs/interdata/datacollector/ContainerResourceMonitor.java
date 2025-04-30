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
        return new Gson().toJson(jsonMap);
    }


}
