package backend.module.voca.service;

import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.domain.customvoca.CustomVocaBook;
import backend.core.domain.customvoca.CustomWord;
import backend.core.domain.user.User;
import backend.core.infra.Snowflake;
import backend.core.infra.repository.CustomVocaBookRepository;
import backend.core.infra.repository.UserRepository;
import backend.module.voca.dto.CustomVocaBookRequest;
import backend.module.voca.dto.CustomVocaBookResponse;
import backend.module.voca.dto.CustomWordRequest;
import backend.module.voca.dto.CustomWordResponse;
import backend.module.voca.repository.CustomWordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomVocaServiceImpl implements CustomVocaService {

    private final UserRepository userRepository;
    private final CustomVocaBookRepository customVocaBookRepository;
    private final CustomWordRepository customWordRepository;
    private final Snowflake snowflake;

    @Override
    @Transactional
    public CustomVocaBookResponse createCustomVocaBook(String email, CustomVocaBookRequest request) {
        User user = findUser(email);
        CustomVocaBook book = customVocaBookRepository.save(CustomVocaBook.builder()
                .customVocaBookId(snowflake.nextId())
                .user(user)
                .name(request.name())
                .build());
        return CustomVocaBookResponse.from(book);
    }

    @Override
    public List<CustomVocaBookResponse> getMyCustomVocaBooks(String email) {
        return customVocaBookRepository.findAllByUserEmail(email).stream()
                .map(CustomVocaBookResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void deleteCustomVocaBook(String email, Long customVocaBookId) {
        CustomVocaBook book = findBookByIdAndEmail(customVocaBookId, email);
        customVocaBookRepository.delete(book);
    }

    @Override
    @Transactional
    public CustomWordResponse addWord(String email, Long customVocaBookId, CustomWordRequest request) {
        CustomVocaBook book = findBookByIdAndEmail(customVocaBookId, email);
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
    public List<CustomWordResponse> getWords(String email, Long customVocaBookId) {
        findBookByIdAndEmail(customVocaBookId, email);
        return customWordRepository.findByCustomVocaBook_CustomVocaBookId(customVocaBookId).stream()
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

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private CustomVocaBook findBookByIdAndEmail(Long customVocaBookId, String email) {
        return customVocaBookRepository.findByCustomVocaBookIdAndUserEmail(customVocaBookId, email)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOM_VOCA_BOOK_NOT_FOUND));
    }

    private CustomWord validateWordOwnership(String email, Long customWordId) {
        CustomWord word = customWordRepository.findByIdWithBookAndUser(customWordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOM_WORD_NOT_FOUND));
        if (!word.getCustomVocaBook().getUser().getEmail().equals(email)) {
            throw new BusinessException(ErrorCode.CUSTOM_VOCA_BOOK_ACCESS_DENIED);
        }
        return word;
    }
}
