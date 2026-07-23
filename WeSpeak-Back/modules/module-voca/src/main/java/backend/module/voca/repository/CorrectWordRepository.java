package backend.module.voca.repository;

import backend.module.voca.domain.CorrectWord;
import backend.module.voca.domain.Test;
import backend.module.voca.domain.Word;
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

    @Modifying
    @Query("delete from CorrectWord cw where cw.word in :words")
    void deleteByWordIn(@Param("words") List<Word> words);

    @Modifying
    @Query("delete from CorrectWord cw where cw.word = :word")
    void deleteByWord(@Param("word") Word word);
}
