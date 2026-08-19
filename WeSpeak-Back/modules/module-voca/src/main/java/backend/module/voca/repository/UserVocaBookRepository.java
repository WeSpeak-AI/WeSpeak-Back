package backend.module.voca.repository;

import backend.module.voca.domain.UserVocaBook;
import backend.module.voca.domain.VocaBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserVocaBookRepository extends JpaRepository<UserVocaBook, Long> {

    void deleteAllByVocaBook(VocaBook vocaBook);

    boolean existsByUserEmailAndVocaBookVocaBookId(String email, Long bookId);

    @Query("select uv from UserVocaBook uv join fetch uv.vocaBook where uv.userEmail = :email")
    List<UserVocaBook> findByUserEmail(@Param("email") String email);

    Optional<UserVocaBook> findByUserEmailAndVocaBookVocaBookId(String email, Long bookId);
}
