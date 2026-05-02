package backend.module.voca.consumer;

import backend.module.voca.dto.VocaGenerationResponse;

public interface VocaSaveService {
    void saveGeneratedVoca(Long vocaId, VocaGenerationResponse response);
}
