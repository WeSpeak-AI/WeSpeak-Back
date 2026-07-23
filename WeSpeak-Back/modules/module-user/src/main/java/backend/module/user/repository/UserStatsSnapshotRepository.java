package backend.module.user.repository;

import backend.module.user.domain.UserStatsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStatsSnapshotRepository extends JpaRepository<UserStatsSnapshot, Long> {

    Optional<UserStatsSnapshot> findByUserId(Long userId);
}
