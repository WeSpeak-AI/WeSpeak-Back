package backend.module.voca.repository;

import backend.core.domain.voca.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByVocaBookDay_VocaBook_VocaBookIdAndVocaBookDay_Day(Long vocaBookId, int day);

    @Query("select w from Word w where w.vocaBookDay.vocaBook.vocaBookId = :bookId and w.vocaBookDay.day between :startDay and :endDay")
    List<Word> findByBookIdAndDayRange(@Param("bookId") Long bookId, @Param("startDay") int startDay, @Param("endDay") int endDay);

    Optional<Word> findByTerm(String term);

    List<Word> findByTermIn(List<String> term);
}
