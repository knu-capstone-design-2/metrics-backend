package kr.cs.interdata.consumer.repository;

import kr.cs.interdata.consumer.entity.LatestAbnormalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LatestAbnormalStatusRepository extends JpaRepository<LatestAbnormalStatus, Integer> {
}
