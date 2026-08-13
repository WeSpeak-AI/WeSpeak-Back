package backend.core.infra.repository;

import backend.core.domain.customvoca.CustomVocaBook;
import backend.core.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomVocaBookRepository extends JpaRepository<CustomVocaBook, Long> {
    List<CustomVocaBook> findAllByUser(User user);
    List<CustomVocaBook> findAllByUserEmail(String email);
    Optional<CustomVocaBook> findByCustomVocaBookIdAndUserEmail(Long customVocaBookId, String email);
}
