package kr.cs.interdata.consumer.service;

import kr.cs.interdata.consumer.repository.MonitoredMachineInventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MachineInventoryService {

    private final MonitoredMachineInventoryRepository monitoredMachineInventoryRepository;

    @Autowired
    public MachineInventoryService(MonitoredMachineInventoryRepository monitoredMachineInventoryRepository) {
        this.monitoredMachineInventoryRepository = monitoredMachineInventoryRepository;
    }

    // 머신 등록 - add
    // 머신 조회
    // 머신 삭제
    // 머신 모든 숫자 조회
    // "type"을 입력한 타입의 머신 총 숫자 조회
}
