package backend.module.voca.consumer;

import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.domain.voca.VocaBook;
import backend.core.domain.voca.VocaBookDay;
import backend.core.domain.voca.Word;
import backend.core.infra.Snowflake;
import backend.module.voca.dto.ImageGenerationResponse;
import backend.module.voca.dto.VocaGenerationResponse;
import backend.module.voca.repository.VocaBookDayRepository;
import backend.module.voca.repository.VocaBookRepository;
import backend.module.voca.repository.WordRepository;
import backend.module.voca.service.R2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VocaSaveServiceImpl implements VocaSaveService {
    private final VocaBookRepository vocaBookRepository;
    private final VocaBookDayRepository vocaBookDayRepository;
    private final WordRepository wordRepository;
    private final Snowflake snowflake;
    private final R2Service r2Service;

    @Transactional
    @Override
    public void saveGeneratedVoca(Long vocaId, VocaGenerationResponse response) {
        VocaBook vocaBook = vocaBookRepository.findById(vocaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOCA_BOOK_NOT_FOUND));

        for (VocaGenerationResponse.DayResult dayResult : response.days()) {
            VocaBookDay vocaBookDay = VocaBookDay.builder()
                    .vocaBookDayId(snowflake.nextId())
                    .vocaBook(vocaBook)
                    .day(dayResult.day())
                    .dayTopic(dayResult.dayTopic())
                    .build();
            vocaBookDayRepository.save(vocaBookDay);
            vocaBook.incrementTotalDays();

            for (VocaGenerationResponse.WordResult wordResult : dayResult.words()) {
                wordRepository.save(Word.builder()
                        .wordId(snowflake.nextId())
                        .vocaBookDay(vocaBookDay)
                        .term(wordResult.term())
                        .meaning(wordResult.meaning())
                        .phonetic(wordResult.phonetic())
                        .example(wordResult.example())
                        .imageUrl(wordResult.imageUrl())
                        .build());
            }
        }
    }

    @Override
    public List<Word> getWords(Long vocaBookId) {
        if (!vocaBookRepository.existsById(vocaBookId)) {
            throw new BusinessException(ErrorCode.VOCA_BOOK_NOT_FOUND);
        }
        return wordRepository.findAllByVocaBookId(vocaBookId);
    }

    @Transactional
    @Override
    public void saveWordImages(List<ImageGenerationResponse.ImageResult> results) {
        for (ImageGenerationResponse.ImageResult result : results) {
            if (result.imageData() == null || result.imageData().isBlank()) continue;
            try {
                String key = "voca-books/" + result.wordId() + ".png";
                byte[] imageBytes = Base64.getDecoder().decode(result.imageData());
                String imageUrl = r2Service.upload(key, imageBytes, "image/png");
                wordRepository.updateImageUrl(result.wordId(), imageUrl);
            } catch (Exception e) {
                log.error("[VocaSaveService] 이미지 저장 실패 wordId={}: {}", result.wordId(), e.getMessage());
            }
        }
    }
}
