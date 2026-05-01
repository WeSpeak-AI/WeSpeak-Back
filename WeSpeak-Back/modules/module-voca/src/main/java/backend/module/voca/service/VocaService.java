package backend.module.voca.service;

import backend.core.domain.voca.VocaBook;
import backend.module.voca.dto.MyVocaBookResponse;
import backend.module.voca.dto.VocaBookDayResponse;
import backend.module.voca.dto.VocaBookResponse;
import backend.module.voca.dto.WordResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface VocaService {

    Page<VocaBookResponse> getAllVocas(int page, int size);

    Page<VocaBookResponse> getVocasByCategory(VocaBook.Category category, int page, int size);

    void startVoca(String email, Long bookId);

    List<MyVocaBookResponse> getMyVocas(String email);

    List<VocaBookDayResponse> getAllDaysByBook(Long bookId);

    List<WordResponse> getWordsByDay(String email, Long bookId, int dayNumber);

    void deleteMyVoca(Long bookId, String email);
}
