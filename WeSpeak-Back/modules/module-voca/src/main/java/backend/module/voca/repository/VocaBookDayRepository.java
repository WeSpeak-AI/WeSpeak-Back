package backend.module.voca.repository;

import backend.module.voca.domain.VocaBookDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VocaBookDayRepository extends JpaRepository<VocaBookDay, Long> {

    List<VocaBookDay> findByVocaBook_VocaBookId(Long bookId);
}
