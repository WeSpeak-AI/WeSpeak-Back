package backend.module.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyPageResponse {

    private String nickname;
    private String email;
    private Stats stats;

    @Getter
    @Builder
    public static class Stats {
        private long vocaWordsTotal;
        private long essayCount;
        private long readingBookCount;
        private long conversationCount;
    }

    public static MyPageResponse of(String nickname, String email,
                                    long vocaWordsTotal, long essayCount,
                                    long readingBookCount, long conversationCount) {
        return MyPageResponse.builder()
                .nickname(nickname)
                .email(email)
                .stats(Stats.builder()
                        .vocaWordsTotal(vocaWordsTotal)
                        .essayCount(essayCount)
                        .readingBookCount(readingBookCount)
                        .conversationCount(conversationCount)
                        .build())
                .build();
    }
}
