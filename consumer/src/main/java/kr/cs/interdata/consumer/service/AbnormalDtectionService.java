package kr.cs.interdata.consumer.service;

import kr.cs.interdata.consumer.repository.AbnormalMetricLogRepository;
import kr.cs.interdata.consumer.repository.LatestAbnormalStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AbnormalDtectionService {

    private final AbnormalMetricLogRepository abnormalMetricLogRepository;
    private final LatestAbnormalStatusRepository latestAbnormalStatusRepository;

    @Autowired
    public AbnormalDtectionService(
            AbnormalMetricLogRepository abnormalMetricLogRepository,
            LatestAbnormalStatusRepository latestAbnormalStatusRepository){
        this.abnormalMetricLogRepository = abnormalMetricLogRepository;
        this.latestAbnormalStatusRepository = latestAbnormalStatusRepository;
    }

    //이상 로그 저장 -> 둘 다 케이스 나눠서 한꺼번에 저장 혹은 업데이트 -  abnormal, latest 둘 다
    //abnormal -> add
    //latest -> add or update
    //최근 이상 상태 조회 - latestAbnormalStauts
    //(선택)1달 이상 지난 로그 삭제 -> 둘 다
}
