package backend.module.voca.repository;

import backend.core.domain.test.CorrectWord;
import backend.core.domain.test.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CorrectWordRepository extends JpaRepository<CorrectWord, Long> {
    List<CorrectWord> findByTest(Test test);

    @Modifying
    @Query("delete from CorrectWord cw where cw.test.testId = :testId")
    void deleteByTestId(@Param("testId") Long testId);
}
