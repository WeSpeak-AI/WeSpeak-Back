package backend.module.voca.repository;

import backend.core.domain.user.User;
import backend.core.domain.uservoca.UserVocaBook;
import backend.core.domain.voca.VocaBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserVocaBookRepository extends JpaRepository<UserVocaBook, Long> {

    boolean existsByUserAndVocaBook(User user, VocaBook vocaBook);

    List<UserVocaBook> findByUser(User user);

    java.util.Optional<UserVocaBook> findByUserAndVocaBook(User user, VocaBook vocaBook);
}
