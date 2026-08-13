package backend.module.voca.repository;

import backend.module.voca.domain.Test;
import backend.module.voca.domain.VocaBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestRepository extends JpaRepository<Test, Long> {

    List<Test> findByVocaBook(VocaBook vocaBook);

    List<Test> findByUserEmailAndVocaBookVocaBookId(String email, Long vocaBookId);

    Optional<Test> findByTestIdAndUserEmail(Long testId, String email);

    List<Test> findByUser_Email(String email);
}
