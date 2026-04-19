package backend.core.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C002", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C003", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "리소스를 찾을 수 없습니다."),

    // Auth
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "존재하지 않는 사용자입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "A002", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "A003", "비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 토큰입니다."),

    // Voca
    VOCA_BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "V001", "단어장을 찾을 수 없습니다."),
    WORD_NOT_FOUND(HttpStatus.NOT_FOUND, "V002", "단어를 찾을 수 없습니다."),
    ALREADY_STARTED_VOCA(HttpStatus.CONFLICT, "V003", "이미 시작한 단어장입니다."),

    // Writing
    ESSAY_NOT_FOUND(HttpStatus.NOT_FOUND, "W001", "에세이를 찾을 수 없습니다."),
    AI_CORRECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "W002", "AI 교정 중 오류가 발생했습니다."),

    // Reading
    READING_BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "지문을 찾을 수 없습니다."),

    // Conversation
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "CN001", "대화 세션을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
