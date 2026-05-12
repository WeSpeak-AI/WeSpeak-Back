package backend.module.voca.service;

import backend.module.voca.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface AdminVocaService {
    Long createVocaBook(VocaBookRequest request);
    void updateVocaBook(Long vocaBookId, VocaBookRequest request);
    String uploadVocaBookImage(Long vocaBookId, MultipartFile file);
    void deleteVocaBook(Long vocaBookId);
    Long createVocaBookDay(Long vocaBookId, VocaBookDayRequest request);
    void updateVocaBookDay(Long vocaBookDayId, VocaBookDayRequest request);
    void deleteVocaBookDay(Long vocaBookDayId);
    Long createWord(Long vocaBookDayId, WordRequest request);
    void updateWord(Long wordId, WordRequest request);
    void deleteWord(Long wordId);
    void aiGenerate(VocaBookRequest request);
    void ingestVoca(IngestRequest request);
}
