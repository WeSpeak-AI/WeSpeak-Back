package backend.module.voca.dto;

import java.util.List;

public record ImageGenerationResponse(List<ImageResult> results) {
    public record ImageResult(Long wordId, String imageData) {}
}
