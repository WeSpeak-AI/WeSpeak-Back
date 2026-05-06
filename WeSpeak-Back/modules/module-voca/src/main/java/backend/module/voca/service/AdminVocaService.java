package backend.module.voca.service;

import backend.module.voca.dto.IngestRequest;
import backend.module.voca.dto.VocaBookDayRequest;
import backend.module.voca.dto.VocaBookRequest;
import backend.module.voca.dto.WordRequest;

public interface AdminVocaService {
    Long createVocaBook(VocaBookRequest request);
    void updateVocaBook(Long vocaBookId, VocaBookRequest request);
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
