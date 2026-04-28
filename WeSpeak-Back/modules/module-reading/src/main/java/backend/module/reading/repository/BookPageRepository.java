package backend.module.reading.repository;

import backend.core.domain.reading.Book;
import backend.core.domain.reading.BookPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookPageRepository extends JpaRepository<BookPage, Long> {

    Optional<BookPage> findByBookAndPageNumber(Book book, int pageNumber);
}
