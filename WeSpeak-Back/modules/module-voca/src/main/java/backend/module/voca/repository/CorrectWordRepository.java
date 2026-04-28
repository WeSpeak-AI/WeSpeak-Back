package backend.module.voca.repository;

import backend.core.domain.test.CorrectWord;
import backend.core.domain.test.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CorrectWordRepository extends JpaRepository<CorrectWord, Long> {
    List<CorrectWord> findByTest(Test test);
}
