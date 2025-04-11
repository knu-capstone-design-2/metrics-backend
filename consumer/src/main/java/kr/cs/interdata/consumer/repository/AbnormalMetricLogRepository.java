package kr.cs.interdata.consumer.repository;

import kr.cs.interdata.consumer.entity.AbnormalMetricLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbnormalMetricLogRepository extends JpaRepository<AbnormalMetricLog, Integer> {
}
