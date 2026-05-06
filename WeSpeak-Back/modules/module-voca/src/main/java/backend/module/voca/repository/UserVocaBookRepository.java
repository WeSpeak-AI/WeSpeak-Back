package backend.module.voca.repository;

import backend.core.domain.user.User;
import backend.core.domain.uservoca.UserVocaBook;
import backend.core.domain.voca.VocaBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserVocaBookRepository extends JpaRepository<UserVocaBook, Long> {

    void deleteAllByVocaBook(VocaBook vocaBook);

    boolean existsByUserAndVocaBook(User user, VocaBook vocaBook);

    boolean existsByUserEmailAndVocaBookVocaBookId(String email, Long bookId);

    List<UserVocaBook> findByUser(User user);

    @Query("select uv from UserVocaBook uv join fetch uv.vocaBook where uv.user.email = :email")
    List<UserVocaBook> findByUserEmail(@Param("email") String email);

    Optional<UserVocaBook> findByUserAndVocaBook(User user, VocaBook vocaBook);

    Optional<UserVocaBook> findByUserEmailAndVocaBookVocaBookId(String email, Long bookId);
}
