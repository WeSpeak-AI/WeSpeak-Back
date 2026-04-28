package backend.module.voca.repository;

import backend.core.domain.customvoca.CustomWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomWordRepository extends JpaRepository<CustomWord, Long> {
    List<CustomWord> findByCustomVocaBook_CustomVocaBookId(Long customVocaBookId);
}
