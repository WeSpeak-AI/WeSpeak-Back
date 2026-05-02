package backend.core.infra.repository;

import backend.core.domain.uservoca.UserVocaBook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserVocaBookStatsRepository extends JpaRepository<UserVocaBook, Long> {
    long countByUserEmail(String email);
}
