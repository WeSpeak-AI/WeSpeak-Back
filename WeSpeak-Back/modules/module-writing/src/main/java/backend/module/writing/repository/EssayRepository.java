package backend.module.writing.repository;

import backend.module.writing.domain.CorrectionStatus;
import backend.module.writing.domain.Essay;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EssayRepository extends JpaRepository<Essay, Long> {

    List<Essay> findByUserEmailOrderByCreatedAtDesc(String email);

    List<Essay> findByCorrectionStatusOrderByCreatedAtAsc(CorrectionStatus correctionStatus, Pageable pageable);
}
