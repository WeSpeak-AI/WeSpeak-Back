package backend.module.writing.repository;

import backend.core.domain.user.User;
import backend.core.domain.writing.Essay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EssayRepository extends JpaRepository<Essay, Long> {

    List<Essay> findByUserOrderByCreatedAtDesc(User user);

    List<Essay> findByUserEmailOrderByCreatedAtDesc(String email);

    Optional<Essay> findByEssayIdAndUser(Long essayId, User user);

    @Query("select e from Essay e join fetch e.user where e.essayId = :essayId")
    Optional<Essay> findByIdWithUser(@Param("essayId") Long essayId);
}
