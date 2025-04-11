package kr.cs.interdata.consumer.repository;

import kr.cs.interdata.consumer.entity.MetricsByType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetricsByTypeRepository extends JpaRepository<MetricsByType, Integer> {
}
