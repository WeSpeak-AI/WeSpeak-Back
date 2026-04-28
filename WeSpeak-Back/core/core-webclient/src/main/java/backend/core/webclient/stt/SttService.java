package backend.core.webclient.stt;

public interface SttService {
    String transcribe(byte[] audioBytes);
}
