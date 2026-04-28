package backend.module.writing.repository;

import backend.core.domain.user.User;
import backend.core.domain.writing.Essay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EssayRepository extends JpaRepository<Essay, Long> {

    List<Essay> findByUserOrderByCreatedAtDesc(User user);

    Optional<Essay> findByEssayIdAndUser(Long essayId, User user);
}
