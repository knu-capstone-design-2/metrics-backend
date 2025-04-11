package kr.cs.interdata.consumer.repository;

import kr.cs.interdata.consumer.entity.MonitoredMachineInventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoredMachineInventoryRepository extends JpaRepository<MonitoredMachineInventory, String> {
}
