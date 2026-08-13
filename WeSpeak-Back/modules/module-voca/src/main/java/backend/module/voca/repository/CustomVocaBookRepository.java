package backend.module.voca.repository;

import backend.module.voca.domain.CustomVocaBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomVocaBookRepository extends JpaRepository<CustomVocaBook, Long> {
    List<CustomVocaBook> findAllByUserEmail(String userEmail);

    Optional<CustomVocaBook> findByCustomVocaBookIdAndUserEmail(Long customVocaBookId, String userEmail);
}
