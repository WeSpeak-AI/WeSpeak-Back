package backend.module.voca.service;

import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.domain.test.CorrectWord;
import backend.core.domain.test.IncorrectWord;
import backend.core.domain.test.Test;
import backend.core.domain.user.User;
import backend.core.domain.voca.VocaBook;
import backend.core.domain.voca.Word;
import backend.core.infra.Snowflake;
import backend.core.infra.repository.UserRepository;
import backend.module.voca.dto.TestRecordPreview;
import backend.module.voca.dto.TestResultRequest;
import backend.module.voca.dto.TestingWordResponse;
import backend.module.voca.dto.WordResultResponse;
import backend.module.voca.repository.CorrectWordRepository;
import backend.module.voca.repository.IncorrectWordRepository;
import backend.module.voca.repository.TestRepository;
import backend.module.voca.repository.UserVocaBookRepository;
import backend.module.voca.repository.VocaBookRepository;
import backend.module.voca.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class TestServiceImpl implements TestService{

    private final WordRepository wordRepository;
    private final TestRepository testRepository;
    private final VocaBookRepository vocaBookRepository;
    private final UserRepository userRepository;
    private final IncorrectWordRepository incorrectWordRepository;
    private final CorrectWordRepository correctWordRepository;
    private final UserVocaBookRepository userVocaBookRepository;
    private final Snowflake snowflake;

    @Override
    public List<TestingWordResponse> startTest(Long bookId, int startDay, int endDay) {
        List<Word> byBookIdAndDayRange = wordRepository.findByBookIdAndDayRange(bookId, startDay, endDay);
        //tts로 word의 term 보내는 로직.
        return byBookIdAndDayRange.stream()
                .map(TestingWordResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void endTest(Long bookId, TestResultRequest testResultRequest, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        VocaBook vocaBook = vocaBookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOCA_BOOK_NOT_FOUND));

        List<String> terms = testResultRequest.getTestedWords().stream()
                .map(TestResultRequest.TestedWord::term)
                .toList();
        List<Word> words = wordRepository.findByTermIn(terms);

        Map<String, Word> wordMap = words.stream()
                .collect(Collectors.toMap(Word::getTerm, word -> word));

        Test test = saveTest(user, vocaBook, testResultRequest);

        updateCurrentDay(user, vocaBook, testResultRequest.getEndDay());
        saveCorrectWords(test, testResultRequest.getTestedWords(), wordMap);
        saveIncorrectWords(test, testResultRequest.getTestedWords(), wordMap);
    }

    private Test saveTest(User user, VocaBook vocaBook, TestResultRequest req) {
        List<TestResultRequest.TestedWord> testedWords = req.getTestedWords();
        int score = (int) testedWords.stream().filter(TestResultRequest.TestedWord::correct).count();

        Test test = Test.builder()
                .testId(snowflake.nextId())
                .user(user)
                .vocaBook(vocaBook)
                .testedAt(LocalDateTime.now())
                .startDay(req.getStartDay())
                .endDay(req.getEndDay())
                .score(score)
                .totalWords(testedWords.size())
                .build();

        return testRepository.save(test);
    }

    private void updateCurrentDay(User user, VocaBook vocaBook, int endDay) {
        userVocaBookRepository.findByUserAndVocaBook(user, vocaBook)
                .ifPresent(uv -> uv.updateCurrentDay(endDay));
    }

    private void saveCorrectWords(Test test, List<TestResultRequest.TestedWord> testedWords, Map<String, Word> wordMap) {
        List<CorrectWord> correctWords = testedWords.stream()
                .filter(TestResultRequest.TestedWord::correct)
                .map(testedWord -> {
                    Word word = wordMap.get(testedWord.term());
                    if (word == null) throw new BusinessException(ErrorCode.WORD_NOT_FOUND);
                    return CorrectWord.builder()
                            .correctWordId(snowflake.nextId())
                            .word(word)
                            .test(test)
                            .build();
                })
                .toList();
        correctWordRepository.saveAll(correctWords);
    }

    private void saveIncorrectWords(Test test, List<TestResultRequest.TestedWord> testedWords, Map<String, Word> wordMap) {
        List<IncorrectWord> incorrectWords = testedWords.stream()
                .filter(w -> !w.correct())
                .map(testedWord -> {
                    Word word = wordMap.get(testedWord.term());
                    if (word == null) throw new BusinessException(ErrorCode.WORD_NOT_FOUND);
                    return IncorrectWord.builder()
                            .incorrectWordId(snowflake.nextId())
                            .word(word)
                            .test(test)
                            .build();
                })
                .toList();
        incorrectWordRepository.saveAll(incorrectWords);
    }

    @Override
    public List<TestRecordPreview> getTestRecordPreviews(String email, Long bookId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        VocaBook vocaBook = vocaBookRepository.findById(bookId)
                .orElseThrow(()-> new BusinessException(ErrorCode.VOCA_BOOK_NOT_FOUND));

        List<Test> tests = testRepository.findByUserAndVocaBook(user, vocaBook);

        return tests.stream()
                .map(TestRecordPreview::from)
                .toList();
    }

    @Override
    public List<WordResultResponse> getIncorrectResult(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEST_NOT_FOUND));

        List<WordResultResponse> results = new java.util.ArrayList<>();
        correctWordRepository.findByTest(test).stream()
                .map(WordResultResponse::fromCorrect)
                .forEach(results::add);
        incorrectWordRepository.findByTest(test).stream()
                .map(WordResultResponse::fromIncorrect)
                .forEach(results::add);
        return results;
    }
}
