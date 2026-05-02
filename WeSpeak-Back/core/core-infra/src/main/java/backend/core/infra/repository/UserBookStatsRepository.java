package backend.core.infra.repository;

import backend.core.domain.userbook.UserBook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBookStatsRepository extends JpaRepository<UserBook, Long> {
    long countByUserEmail(String email);
}
