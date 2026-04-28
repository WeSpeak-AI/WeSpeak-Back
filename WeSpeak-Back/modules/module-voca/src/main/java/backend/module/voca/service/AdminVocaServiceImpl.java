package backend.module.voca.service;

import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.domain.voca.VocaBook;
import backend.core.domain.voca.VocaBookDay;
import backend.core.domain.voca.Word;
import backend.core.infra.Snowflake;
import backend.module.voca.dto.VocaBookDayRequest;
import backend.module.voca.dto.VocaBookRequest;
import backend.module.voca.dto.WordRequest;
import backend.module.voca.repository.VocaBookDayRepository;
import backend.module.voca.repository.VocaBookRepository;
import backend.module.voca.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminVocaServiceImpl implements AdminVocaService {

    private final VocaBookRepository vocaBookRepository;
    private final VocaBookDayRepository vocaBookDayRepository;
    private final WordRepository wordRepository;
    private final Snowflake snowflake;

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
    public void deleteVocaBook(Long vocaBookId) {
        VocaBook vocaBook = vocaBookRepository.findById(vocaBookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOCA_BOOK_NOT_FOUND));
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
        wordRepository.delete(word);
    }
}
