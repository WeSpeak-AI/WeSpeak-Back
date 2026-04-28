package backend.core.infra.repository;

import backend.core.domain.customvoca.CustomVocaBook;
import backend.core.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomVocaBookRepository extends JpaRepository<CustomVocaBook, Long> {
    Optional<CustomVocaBook> findByUser(User user);
}
