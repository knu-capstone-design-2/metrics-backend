package kr.cs.interdata.datacollector;
import java.io.File;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DataCollectorApplication {

    public static void main(String[] args) {

            //String filePath = "C:/Users/chaee/Desktop/knu-capstone-design2/metrics-backend/data-collector/test.txt";

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

    }
}