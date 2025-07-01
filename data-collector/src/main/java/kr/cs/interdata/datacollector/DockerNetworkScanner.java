package kr.cs.interdata.datacollector;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.core.DockerClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class DockerNetworkScanner {
    public static Set<String> getAllConnectedContainerIds() {
        Set<String> containerIds = null;
        List<Network> networks = null;

        final Logger logger = LoggerFactory.getLogger(DockerNetworkScanner.class);

        try (DockerClient dockerClient = DockerClientBuilder.getInstance().build()) {
            containerIds = new HashSet<>();

            // 1. 모든 네트워크 조회
            networks = dockerClient.listNetworksCmd().exec();
        } catch (IOException e) {
            logger.warn("도커 네트워크 정보를 불러오는 중 예외 발생. 도커 데몬이 실행 중인지 확인하세요.", e);
        }
        for (Network network : networks) {
            // 2. 네트워크에 연결된 컨테이너들 조회
            Map<String, Network.ContainerNetworkConfig> containers = network.getContainers();
            if (containers != null) {
                containerIds.addAll(containers.keySet());
            }
        }
        return containerIds;
    }

    public static void main(String[] args) {
        Set<String> allContainerIds = getAllConnectedContainerIds();
        System.out.println("모든 네트워크에 연결된 컨테이너 ID 목록:");
        for (String id : allContainerIds) {
            System.out.println(id);
        }
    }

}
