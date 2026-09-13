package backend.module.writing.consumer;

import backend.core.common.event.Event;
import backend.core.common.event.EventType;
import backend.core.common.event.payload.AiCorrectionEventPayload;
import backend.core.common.event.payload.UserDeletedEventPayload;
import backend.module.writing.domain.CorrectionStatus;
import backend.module.writing.domain.Essay;
import backend.module.writing.repository.EssayRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class AiCorrectionDltConsumerTest {

    private final EssayRepository essayRepository = mock(EssayRepository.class);
    private final AiCorrectionSaveService saveService = new AiCorrectionSaveServiceImpl(essayRepository);
    private final AiCorrectionDltConsumer consumer = new AiCorrectionDltConsumer(saveService);
    private final Acknowledgment ack = mock(Acknowledgment.class);

    private ConsumerRecord<String, String> dltRecord(String value) {
        return new ConsumerRecord<>("writing.DLT", 0, 0L, null, value);
    }

    private String aiCorrectionEvent(Long essayId) {
        return Event.createEvent(10L, EventType.AI_CORRECTION, AiCorrectionEventPayload.builder()
                .essayId(essayId)
                .content("content")
                .build()).toJson();
    }

    private Essay essay(CorrectionStatus status) {
        return Essay.builder()
                .essayId(1L).userEmail("user@test.com").topic("topic").content("content")
                .type(Essay.Type.ESSAY).correctionStatus(status)
                .build();
    }

    @Test
    void marksPendingEssayAsFailedAndAcks() {
        Essay essay = essay(CorrectionStatus.PENDING);
        when(essayRepository.findById(1L)).thenReturn(Optional.of(essay));

        consumer.listen(dltRecord(aiCorrectionEvent(1L)), ack);

        assertThat(essay.getCorrectionStatus()).isEqualTo(CorrectionStatus.FAILED);
        verify(ack).acknowledge();
    }

    @Test
    void keepsCompletedEssayAsIsAndAcks() {
        Essay essay = essay(CorrectionStatus.COMPLETED);
        when(essayRepository.findById(1L)).thenReturn(Optional.of(essay));

        consumer.listen(dltRecord(aiCorrectionEvent(1L)), ack);

        assertThat(essay.getCorrectionStatus()).isEqualTo(CorrectionStatus.COMPLETED);
        verify(ack).acknowledge();
    }

    @Test
    void deletedEssayDoesNotThrowAndAcks() {
        when(essayRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatCode(() -> consumer.listen(dltRecord(aiCorrectionEvent(99L)), ack)).doesNotThrowAnyException();
        verify(ack).acknowledge();
    }

    @Test
    void nonAiCorrectionEventIsSkipped() {
        String otherEvent = Event.createEvent(11L, EventType.USER_DELETED,
                UserDeletedEventPayload.builder().email("user@test.com").build()).toJson();

        consumer.listen(dltRecord(otherEvent), ack);

        verifyNoInteractions(essayRepository);
        verify(ack).acknowledge();
    }
}
