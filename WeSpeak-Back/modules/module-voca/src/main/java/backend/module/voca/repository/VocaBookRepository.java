package backend.module.voca.repository;

import backend.module.voca.domain.VocaBook.Category;
import backend.module.voca.domain.VocaBook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VocaBookRepository extends JpaRepository<VocaBook, Long> {
    Page<VocaBook> findByCategory(Category tag, Pageable pageable);
}
