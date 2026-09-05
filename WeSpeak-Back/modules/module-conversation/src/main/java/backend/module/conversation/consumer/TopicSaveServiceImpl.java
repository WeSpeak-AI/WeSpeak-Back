package backend.module.conversation.consumer;

import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.module.conversation.domain.Topic;
import backend.module.conversation.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicSaveServiceImpl implements TopicSaveService {

    private final TopicRepository topicRepository;

    @Override
    @Transactional
    public void applyContent(Long topicId, String content) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND));
        topic.applyContent(content);
    }
}
