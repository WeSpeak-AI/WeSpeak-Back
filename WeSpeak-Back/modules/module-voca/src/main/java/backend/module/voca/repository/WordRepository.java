package backend.module.voca.repository;

import backend.module.voca.domain.VocaBookDay;
import backend.module.voca.domain.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByVocaBookDay(VocaBookDay vocaBookDay);

    List<Word> findByVocaBookDay_VocaBook_VocaBookIdAndVocaBookDay_Day(Long vocaBookId, int day);

    @Query("select w from Word w where w.vocaBookDay.vocaBook.vocaBookId = :bookId and w.vocaBookDay.day between :startDay and :endDay")
    List<Word> findByBookIdAndDayRange(@Param("bookId") Long bookId, @Param("startDay") int startDay, @Param("endDay") int endDay);

    Optional<Word> findByTerm(String term);

    List<Word> findByTermIn(List<String> term);

    @Query("select w from Word w where w.vocaBookDay.vocaBook.vocaBookId = :bookId and w.term in :terms")
    List<Word> findByBookIdAndTermIn(@Param("bookId") Long bookId, @Param("terms") List<String> terms);

    @Query("select w from Word w where w.vocaBookDay.vocaBook.vocaBookId = :vocaBookId")
    List<Word> findAllByVocaBookId(@Param("vocaBookId") Long vocaBookId);

    @Modifying
    @Query("update Word w set w.imageUrl = :imageUrl where w.wordId = :wordId")
    void updateImageUrl(@Param("wordId") Long wordId, @Param("imageUrl") String imageUrl);
}
