package backend.module.reading.repository;

import backend.core.domain.reading.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingBookRepository extends JpaRepository<Book, Long> {

    Page<Book> findByLevel(Book.Level level, Pageable pageable);
}
