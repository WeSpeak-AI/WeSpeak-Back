package backend.module.conversation.scheduler;

import backend.module.conversation.domain.Conversation;
import backend.module.conversation.repository.ConversationMessageRepository;
import backend.module.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationCloseScheduler {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String REDIS_KEY = "conversation:history:";

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void deleteExpiredConversations() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
        List<Conversation> expired = conversationRepository
                .findByStatusAndStartedAtBefore(Conversation.Status.ACTIVE, threshold);

        if (expired.isEmpty()) return;

        conversationMessageRepository.deleteAllByConversationIn(expired);
        conversationRepository.deleteAll(expired);
        expired.forEach(c -> redisTemplate.delete(REDIS_KEY + c.getConversationId()));

        log.info("[ConversationCloseScheduler] {}개의 만료된 대화 삭제", expired.size());
    }
}
