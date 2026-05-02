package backend.core.infra.repository;

import backend.core.domain.writing.Essay;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EssayStatsRepository extends JpaRepository<Essay, Long> {
    long countByUserEmail(String email);
}
