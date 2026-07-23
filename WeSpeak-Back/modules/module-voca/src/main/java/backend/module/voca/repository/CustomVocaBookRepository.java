package backend.module.voca.repository;

import backend.module.voca.domain.CustomVocaBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomVocaBookRepository extends JpaRepository<CustomVocaBook, Long> {
    Optional<CustomVocaBook> findByUserEmail(String userEmail);
}
