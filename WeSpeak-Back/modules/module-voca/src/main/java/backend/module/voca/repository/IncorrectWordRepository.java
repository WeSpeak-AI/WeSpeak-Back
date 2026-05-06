package backend.module.voca.repository;


import backend.core.domain.test.IncorrectWord;
import backend.core.domain.test.Test;
import backend.core.domain.voca.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncorrectWordRepository extends JpaRepository<IncorrectWord, Long> {
    List<IncorrectWord> findByTest(Test test);

    @Modifying
    @Query("delete from IncorrectWord iw where iw.test.testId = :testId")
    void deleteByTestId(@Param("testId") Long testId);

    @Modifying
    @Query("delete from IncorrectWord iw where iw.word in :words")
    void deleteByWordIn(@Param("words") List<Word> words);

    @Modifying
    @Query("delete from IncorrectWord iw where iw.word = :word")
    void deleteByWord(@Param("word") Word word);
}
