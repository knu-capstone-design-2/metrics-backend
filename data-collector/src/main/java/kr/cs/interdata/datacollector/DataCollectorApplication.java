package kr.cs.interdata.datacollector;
import java.io.File;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DataCollectorApplication {

    public static void main(String[] args) {

        String filePath = "C:/Users/chaee/Desktop/knu-capstone-design2/metrics-backend/data-collector/test.txt";

        File file = new File(filePath);
        System.out.println("파일 존재 여부: " + file.exists());
        System.out.println("절대 경로: " + file.getAbsolutePath());

        String fileContent = ContainerResourceMonitor.readFile(filePath);
        System.out.println("readFile 결과: " + fileContent);

        Long longValue = ContainerResourceMonitor.readLongFromFile(filePath);
        System.out.println("readLongFromFile 결과: " + longValue);

        System.out.println("현재 작업 디렉토리: " + System.getProperty("user.dir"));

        // 스프링 부트 애플리케이션 실행
        //SpringApplication.run(DataCollectorApplication.class, args);

        //String json = ContainerResourceMonitor.collectContainerResources();
        //System.out.println("=== 컨테이너 리소스 수집 결과 ===");
        //System.out.println(json);
    }
}