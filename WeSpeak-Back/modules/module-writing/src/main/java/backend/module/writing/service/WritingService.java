package backend.module.writing.service;

import backend.module.writing.dto.CorrectionResponse;
import backend.module.writing.dto.EssayRequest;
import backend.module.writing.dto.EssayResponse;

import java.util.List;

public interface WritingService {

    EssayResponse save(String email, EssayRequest request);

    List<EssayResponse> getMyEssays(String email);

    CorrectionResponse getMyEssay(Long essayId);

    String getRandomTopic();

    void deleteMyEssay(String email, Long essayId);
}
