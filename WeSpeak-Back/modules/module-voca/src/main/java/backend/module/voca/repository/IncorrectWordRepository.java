package backend.module.voca.repository;


import backend.core.domain.test.IncorrectWord;
import backend.core.domain.test.Test;
import backend.core.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncorrectWordRepository extends JpaRepository<IncorrectWord, Long> {
    List<IncorrectWord> findByTest(Test test);
}
