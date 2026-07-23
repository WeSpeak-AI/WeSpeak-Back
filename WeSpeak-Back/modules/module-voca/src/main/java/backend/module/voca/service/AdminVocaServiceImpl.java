package backend.module.voca.service;

import backend.core.common.event.EventType;
import backend.core.common.event.payload.VocaGenerationEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.common.outboxmessagerelay.pub.OutboxEventPublisher;
import backend.module.voca.domain.Test;
import backend.module.voca.domain.VocaBook;
import backend.module.voca.domain.VocaBookDay;
import backend.module.voca.domain.Word;
import backend.core.infra.Snowflake;
import backend.module.voca.dto.*;
import backend.module.voca.repository.CorrectWordRepository;
import backend.module.voca.repository.IncorrectWordRepository;
import backend.module.voca.repository.TestRepository;
import backend.module.voca.repository.UserVocaBookRepository;
import backend.module.voca.repository.VocaBookDayRepository;
import backend.module.voca.repository.VocaBookRepository;
import backend.module.voca.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminVocaServiceImpl implements AdminVocaService {

    @Qualifier("aiWebClient")
    private final WebClient aiWebClient;
    private final VocaBookRepository vocaBookRepository;
    private final VocaBookDayRepository vocaBookDayRepository;
    private final WordRepository wordRepository;
    private final TestRepository testRepository;
    private final CorrectWordRepository correctWordRepository;
    private final IncorrectWordRepository incorrectWordRepository;
    private final UserVocaBookRepository userVocaBookRepository;
    private final Snowflake snowflake;
    private final OutboxEventPublisher outboxEventPublisher;
    private final R2Service r2Service;

    @Override
    @Transactional
    public Long createVocaBook(VocaBookRequest request) {
        VocaBook vocaBook = VocaBook.builder()
                .vocaBookId(snowflake.nextId())
                .title(request.getTitle())
                .category(request.getCategory())
                .description(request.getDescription())
                .build();
        return vocaBookRepository.save(vocaBook).getVocaBookId();
    }

    @Override
    @Transactional
    public void updateVocaBook(Long vocaBookId, VocaBookRequest request) {
        VocaBook vocaBook = vocaBookRepository.findById(vocaBookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOCA_BOOK_NOT_FOUND));
        vocaBook.update(request.getTitle(), request.getCategory(), request.getDescription());
    }

    @Override
    @Transactional
    public String uploadVocaBookImage(Long vocaBookId, MultipartFile file) {
        VocaBook vocaBook = vocaBookRepository.findById(vocaBookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOCA_BOOK_NOT_FOUND));
        try {
            String key = "voca-books/" + vocaBookId + "/" + file.getOriginalFilename();
            String url = r2Service.upload(key, file.getBytes(), file.getContentType());
            vocaBook.updateImage(url);
            return url;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public void deleteVocaBook(Long vocaBookId) {
        VocaBook vocaBook = vocaBookRepository.findById(vocaBookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOCA_BOOK_NOT_FOUND));
        List<Test> tests = testRepository.findByVocaBook(vocaBook);
        for (Test test : tests) {
            correctWordRepository.deleteByTestId(test.getTestId());
            incorrectWordRepository.deleteByTestId(test.getTestId());
        }
        testRepository.deleteAll(tests);
        userVocaBookRepository.deleteAllByVocaBook(vocaBook);
        vocaBookRepository.delete(vocaBook);
    }

    @Override
    @Transactional
    public Long createVocaBookDay(Long vocaBookId, VocaBookDayRequest request) {
        VocaBook vocaBook = vocaBookRepository.findById(vocaBookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOCA_BOOK_NOT_FOUND));
        VocaBookDay vocaBookDay = VocaBookDay.builder()
                .vocaBookDayId(snowflake.nextId())
                .vocaBook(vocaBook)
                .day(request.getDay())
                .dayTopic(request.getDayTopic())
                .build();
        vocaBook.incrementTotalDays();
        return vocaBookDayRepository.save(vocaBookDay).getVocaBookDayId();
    }

    @Override
    @Transactional
    public void updateVocaBookDay(Long vocaBookDayId, VocaBookDayRequest request) {
        VocaBookDay vocaBookDay = vocaBookDayRepository.findById(vocaBookDayId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOCA_BOOK_DAY_NOT_FOUND));
        vocaBookDay.update(request.getDay(), request.getDayTopic());
    }

    @Override
    @Transactional
    public void deleteVocaBookDay(Long vocaBookDayId) {
        VocaBookDay vocaBookDay = vocaBookDayRepository.findById(vocaBookDayId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOCA_BOOK_DAY_NOT_FOUND));
        List<Word> words = wordRepository.findByVocaBookDay(vocaBookDay);
        if (!words.isEmpty()) {
            correctWordRepository.deleteByWordIn(words);
            incorrectWordRepository.deleteByWordIn(words);
        }
        vocaBookDay.getVocaBook().decrementTotalDays();
        vocaBookDayRepository.delete(vocaBookDay);
    }

    @Override
    @Transactional
    public Long createWord(Long vocaBookDayId, WordRequest request) {
        VocaBookDay vocaBookDay = vocaBookDayRepository.findById(vocaBookDayId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOCA_BOOK_DAY_NOT_FOUND));
        Word word = Word.builder()
                .wordId(snowflake.nextId())
                .vocaBookDay(vocaBookDay)
                .term(request.getTerm())
                .meaning(request.getMeaning())
                .phonetic(request.getPhonetic())
                .example(request.getExample())
                .imageUrl(request.getImageUrl())
                .build();
        return wordRepository.save(word).getWordId();
    }

    @Override
    @Transactional
    public void updateWord(Long wordId, WordRequest request) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORD_NOT_FOUND));
        word.update(request.getTerm(), request.getMeaning(), request.getPhonetic(), request.getExample(), request.getImageUrl());
    }

    @Override
    @Transactional
    public void deleteWord(Long wordId) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORD_NOT_FOUND));
        correctWordRepository.deleteByWord(word);
        incorrectWordRepository.deleteByWord(word);
        wordRepository.delete(word);
    }

    @Override
    public void ingestVoca(IngestRequest request) {
        aiWebClient.post()
                .uri("/admin/ingest/voca")
                .bodyValue(Map.of("files", request.getFiles()))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    @Override
    @Transactional
    public void aiGenerate(VocaBookRequest request) {
        VocaBook vocaBook = vocaBookRepository.save(VocaBook.builder()
                .vocaBookId(snowflake.nextId())
                .title(request.getTitle())
                .category(request.getCategory())
                .description(request.getDescription())
                .build());
        outboxEventPublisher.publish(EventType.VOCA_GENERATION, VocaGenerationEventPayload.builder()
                .vocaId(vocaBook.getVocaBookId())
                .title(vocaBook.getTitle())
                .category(vocaBook.getCategory().name())
                .description(vocaBook.getDescription())
                .numberOfDays(request.getNumberOfDays())
                .build());
    }
}
