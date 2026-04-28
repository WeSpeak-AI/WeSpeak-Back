package backend.module.voca.service;

import backend.module.voca.dto.CustomVocaBookResponse;
import backend.module.voca.dto.CustomWordRequest;
import backend.module.voca.dto.CustomWordResponse;

import java.util.List;

public interface CustomVocaService {

    CustomVocaBookResponse getMyCustomVocaBook(String email);

    CustomWordResponse addWord(String email, CustomWordRequest request);

    List<CustomWordResponse> getWords(String email);

    CustomWordResponse updateWord(String email, Long customWordId, CustomWordRequest request);

    void deleteWord(String email, Long customWordId);
}
