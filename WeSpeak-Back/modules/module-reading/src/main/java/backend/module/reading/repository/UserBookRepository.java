package backend.module.reading.repository;

import backend.core.domain.reading.Book;
import backend.core.domain.user.User;
import backend.core.domain.userbook.UserBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserBookRepository extends JpaRepository<UserBook, Long> {

    Optional<UserBook> findByUserAndBook(User user, Book book);

    @Query("select ub from UserBook ub " +
            "join fetch ub.book " +
            "where ub.user = :user")
    List<UserBook> findByUser(@Param("user")User user);

    boolean existsByBook(Book book);
}
