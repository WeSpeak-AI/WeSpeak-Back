package backend.module.writing.service;

public interface AdminWritingService {

    void retryCorrection(Long essayId);

    int retryFailedCorrections();
}
