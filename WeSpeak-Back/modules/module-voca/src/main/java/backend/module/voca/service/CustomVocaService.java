package backend.module.voca.service;

import backend.module.voca.dto.CustomVocaBookRequest;
import backend.module.voca.dto.CustomVocaBookResponse;
import backend.module.voca.dto.CustomWordRequest;
import backend.module.voca.dto.CustomWordResponse;

import java.util.List;

public interface CustomVocaService {

    CustomVocaBookResponse createCustomVocaBook(String email, CustomVocaBookRequest request);

    List<CustomVocaBookResponse> getMyCustomVocaBooks(String email);

    void deleteCustomVocaBook(String email, Long customVocaBookId);

    CustomWordResponse addWord(String email, Long customVocaBookId, CustomWordRequest request);

    List<CustomWordResponse> getWords(String email, Long customVocaBookId);

    CustomWordResponse updateWord(String email, Long customWordId, CustomWordRequest request);

    void deleteWord(String email, Long customWordId);
}
