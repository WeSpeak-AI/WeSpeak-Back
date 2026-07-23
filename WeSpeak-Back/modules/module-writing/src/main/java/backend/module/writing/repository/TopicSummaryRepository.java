package backend.module.writing.repository;

import backend.module.writing.domain.TopicSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TopicSummaryRepository extends JpaRepository<TopicSummary, Long> {

    @Query(value = "SELECT * FROM topic_summary ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<TopicSummary> findRandom();
}
