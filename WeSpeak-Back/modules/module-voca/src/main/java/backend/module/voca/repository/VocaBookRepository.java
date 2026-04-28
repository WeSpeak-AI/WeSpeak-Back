package backend.module.voca.repository;

import backend.core.domain.user.User;
import backend.core.domain.voca.VocaBook.Category;
import backend.core.domain.voca.VocaBook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VocaBookRepository extends JpaRepository<VocaBook, Long> {
    Page<VocaBook> findByCategory(Category tag, Pageable pageable);
}
