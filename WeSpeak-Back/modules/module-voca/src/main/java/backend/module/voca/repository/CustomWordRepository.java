package backend.module.voca.repository;

import backend.core.domain.customvoca.CustomWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomWordRepository extends JpaRepository<CustomWord, Long> {
    List<CustomWord> findByCustomVocaBook_CustomVocaBookId(Long customVocaBookId);

    @Query("select cw from CustomWord cw " +
            "join fetch cw.customVocaBook cb " +
            "join fetch cb.user " +
            "where cw.customWordId = :wordId")
    Optional<CustomWord> findByIdWithBookAndUser(@Param("wordId") Long wordId);
}
