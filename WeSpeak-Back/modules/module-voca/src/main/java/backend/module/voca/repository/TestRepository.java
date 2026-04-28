package backend.module.voca.repository;

import backend.core.domain.test.Test;
import backend.core.domain.user.User;
import backend.core.domain.voca.VocaBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestRepository extends JpaRepository<Test, Long> {

    List<Test> findByUserAndVocaBook(User user, VocaBook vocaBook);

}
