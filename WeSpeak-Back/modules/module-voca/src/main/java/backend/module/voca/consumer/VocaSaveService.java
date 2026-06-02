package backend.module.voca.consumer;

import backend.core.domain.voca.Word;
import backend.module.voca.dto.ImageGenerationResponse;
import backend.module.voca.dto.VocaGenerationResponse;

import java.util.List;

public interface VocaSaveService {
    void saveGeneratedVoca(Long vocaId, VocaGenerationResponse response);

    List<Word> getWords(Long vocaBookId);

    void saveWordImages(List<ImageGenerationResponse.ImageResult> results);
}
