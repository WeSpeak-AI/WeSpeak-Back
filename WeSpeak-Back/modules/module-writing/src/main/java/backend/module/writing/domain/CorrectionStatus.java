package backend.module.writing.domain;

public enum CorrectionStatus {
    PENDING,    // AI 교정 요청됨 (처리 중 또는 재시도 중)
    COMPLETED,  // AI 교정 완료
    FAILED      // 재시도를 모두 소진해 DLT로 이동함 — 운영자 재처리 대상
}
