package backend.module.voca.consumer;

import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.domain.voca.VocaBook;
import backend.core.domain.voca.VocaBookDay;
import backend.core.domain.voca.Word;
import backend.core.infra.Snowflake;
import backend.module.voca.dto.VocaGenerationResponse;
import backend.module.voca.repository.VocaBookDayRepository;
import backend.module.voca.repository.VocaBookRepository;
import backend.module.voca.repository.WordRepository;
import backend.module.voca.service.VocaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VocaSaveServiceImpl implements VocaSaveService {
    private final VocaBookRepository vocaBookRepository;
    private final VocaBookDayRepository vocaBookDayRepository;
    private final WordRepository wordRepository;
    private final Snowflake snowflake;

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
}
