package backend.module.user.scheduler;

import backend.module.user.domain.UserStatsSnapshot;
import backend.module.user.repository.UserRepository;
import backend.module.user.repository.UserStatsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * user_stats_snapshot에 대한 자체 일관성 점검. voca/writing/reading/conversation의 실제 카운트와
 * 대조하는 진짜 교차 서비스 정합성 검증이 아니라(그러려면 각 서비스에 신규 내부 조회 API가
 * 필요해 범위 밖으로 분리, research.md §9 참고), user-service 내부에서만 확인 가능한 이상
 * 징후만 로그로 남긴다(FR-009 부분 충족).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsReconciliationScheduler {

    private final UserStatsSnapshotRepository userStatsSnapshotRepository;
    private final UserRepository userRepository;

    @Scheduled(fixedDelay = 3600000)
    @Transactional(readOnly = true)
    public void checkSelfConsistency() {
        int negativeCount = 0;
        int orphanCount = 0;

        for (UserStatsSnapshot snapshot : userStatsSnapshotRepository.findAll()) {
            if (snapshot.getVocaBookCount() < 0 || snapshot.getEssayCount() < 0
                    || snapshot.getUserBookCount() < 0 || snapshot.getConversationCount() < 0) {
                log.warn("[StatsReconciliationScheduler] negative count detected: userId={}, snapshot={}",
                        snapshot.getUserId(), snapshot);
                negativeCount++;
            }
            if (!userRepository.existsById(snapshot.getUserId())) {
                log.warn("[StatsReconciliationScheduler] orphaned snapshot (no matching User): userId={}",
                        snapshot.getUserId());
                orphanCount++;
            }
        }

        if (negativeCount > 0 || orphanCount > 0) {
            log.warn("[StatsReconciliationScheduler] anomalies found: negativeCount={}, orphanCount={}",
                    negativeCount, orphanCount);
        }
    }
}
