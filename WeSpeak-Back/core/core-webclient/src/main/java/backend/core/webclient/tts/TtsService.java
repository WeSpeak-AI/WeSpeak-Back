package backend.core.webclient.tts;

import java.io.IOException;
import java.io.OutputStream;

public interface TtsService {

    String generateUrl(String text);

    void stream(String text, OutputStream outputStream) throws IOException;
}
