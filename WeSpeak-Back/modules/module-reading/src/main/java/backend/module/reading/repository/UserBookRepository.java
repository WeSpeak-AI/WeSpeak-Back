package backend.module.reading.repository;

import backend.module.reading.domain.UserBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserBookRepository extends JpaRepository<UserBook, Long> {

    boolean existsByBookBookId(Long bookId);

    @Query("select ub from UserBook ub join fetch ub.book where ub.userEmail = :email")
    List<UserBook> findByUserEmail(@Param("email") String email);

    Optional<UserBook> findByUserEmailAndBookBookId(String email, Long bookId);
}
