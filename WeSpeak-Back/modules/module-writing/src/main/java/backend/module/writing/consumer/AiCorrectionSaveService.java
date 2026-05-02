package backend.module.writing.consumer;

public interface AiCorrectionSaveService {
    void applyAiCorrection(Long essayId, String correctionResult);
}
