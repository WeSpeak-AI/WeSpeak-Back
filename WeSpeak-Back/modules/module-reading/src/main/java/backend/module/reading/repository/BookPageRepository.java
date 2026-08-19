package backend.module.reading.repository;

import backend.module.reading.domain.Book;
import backend.module.reading.domain.BookPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookPageRepository extends JpaRepository<BookPage, Long> {

    void deleteAllByBook(Book book);

    Optional<BookPage> findByBookAndPageNumber(Book book, int pageNumber);

    @Query("select bp from BookPage bp where bp.book.bookId = :bookId and bp.pageNumber = :pageNumber")
    Optional<BookPage> findByBookIdAndPageNumber(@Param("bookId") Long bookId, @Param("pageNumber") int pageNumber);
}
