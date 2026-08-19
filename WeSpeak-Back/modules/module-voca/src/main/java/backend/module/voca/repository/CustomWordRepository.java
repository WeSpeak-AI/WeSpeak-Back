package backend.module.voca.repository;

import backend.module.voca.domain.CustomWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomWordRepository extends JpaRepository<CustomWord, Long> {
    List<CustomWord> findByCustomVocaBook_CustomVocaBookId(Long customVocaBookId);

    @Query("select cw from CustomWord cw " +
            "join fetch cw.customVocaBook cb " +
            "where cw.customWordId = :wordId")
    Optional<CustomWord> findByIdWithBook(@Param("wordId") Long wordId);
}
