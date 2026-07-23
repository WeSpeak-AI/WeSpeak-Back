package backend.module.voca.service;

import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.module.voca.domain.CustomVocaBook;
import backend.module.voca.domain.CustomWord;
import backend.core.infra.Snowflake;
import backend.module.voca.repository.CustomVocaBookRepository;
import backend.module.voca.dto.CustomVocaBookResponse;
import backend.module.voca.dto.CustomWordRequest;
import backend.module.voca.dto.CustomWordResponse;
import backend.module.voca.repository.CustomWordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomVocaServiceImpl implements CustomVocaService {

    private final CustomVocaBookRepository customVocaBookRepository;
    private final CustomWordRepository customWordRepository;
    private final Snowflake snowflake;

    @Override
    @Transactional
    public CustomVocaBookResponse getMyCustomVocaBook(String email) {
        try {
            CustomVocaBook book = customVocaBookRepository.findByUserEmail(email)
                    .orElseGet(() -> customVocaBookRepository.save(CustomVocaBook.builder()
                            .customVocaBookId(snowflake.nextId())
                            .userEmail(email)
                            .build()));
            return CustomVocaBookResponse.from(book);
        } catch (DataIntegrityViolationException e) {
            return CustomVocaBookResponse.from(
                    customVocaBookRepository.findByUserEmail(email)
                            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOM_VOCA_BOOK_NOT_FOUND))
            );
        }
    }

    @Override
    @Transactional
    public CustomWordResponse addWord(String email, CustomWordRequest request) {
        CustomVocaBook book = getOrCreateBook(email);
        CustomWord word = customWordRepository.save(CustomWord.builder()
                .customWordId(snowflake.nextId())
                .customVocaBook(book)
                .term(request.term())
                .meaning(request.meaning())
                .phonetic(request.phonetic())
                .example(request.example())
                .imageUrl(request.imageUrl())
                .build());
        return CustomWordResponse.from(word);
    }

    @Override
    public List<CustomWordResponse> getWords(String email) {
        CustomVocaBook book = customVocaBookRepository.findByUserEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOM_VOCA_BOOK_NOT_FOUND));
        return customWordRepository.findByCustomVocaBook_CustomVocaBookId(book.getCustomVocaBookId()).stream()
                .map(CustomWordResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public CustomWordResponse updateWord(String email, Long customWordId, CustomWordRequest request) {
        CustomWord word = validateWordOwnership(email, customWordId);
        word.update(request.term(), request.meaning(), request.phonetic(), request.example(), request.imageUrl());
        return CustomWordResponse.from(word);
    }

    @Override
    @Transactional
    public void deleteWord(String email, Long customWordId) {
        CustomWord word = validateWordOwnership(email, customWordId);
        customWordRepository.delete(word);
    }

    private CustomVocaBook getOrCreateBook(String email) {
        try {
            return customVocaBookRepository.findByUserEmail(email)
                    .orElseGet(() -> customVocaBookRepository.save(CustomVocaBook.builder()
                            .customVocaBookId(snowflake.nextId())
                            .userEmail(email)
                            .build()));
        } catch (DataIntegrityViolationException e) {
            return customVocaBookRepository.findByUserEmail(email)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOM_VOCA_BOOK_NOT_FOUND));
        }
    }

    private CustomWord validateWordOwnership(String email, Long customWordId) {
        CustomWord word = customWordRepository.findByIdWithBook(customWordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOM_WORD_NOT_FOUND));
        if (!word.getCustomVocaBook().getUserEmail().equals(email)) {
            throw new BusinessException(ErrorCode.CUSTOM_VOCA_BOOK_ACCESS_DENIED);
        }
        return word;
    }
}
