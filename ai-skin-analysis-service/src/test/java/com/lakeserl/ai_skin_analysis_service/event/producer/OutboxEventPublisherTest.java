package com.lakeserl.ai_skin_analysis_service.event.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.ai_skin_analysis_service.entity.OutboxEvent;
import com.lakeserl.ai_skin_analysis_service.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The outbox poller must mark a row published ONLY after the broker acknowledges — otherwise a
 * failed send would silently drop the event. On failure the row stays unpublished for retry.
 */
class OutboxEventPublisherTest {

    private OutboxRepository outboxRepository;
    private AIEventProducer eventProducer;
    private OutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(OutboxRepository.class);
        eventProducer = mock(AIEventProducer.class);
        publisher = new OutboxEventPublisher(outboxRepository, eventProducer, new ObjectMapper());
    }

    private OutboxEvent pendingEvent() {
        return OutboxEvent.builder()
                .aggregateId("s1")
                .eventType(AIEventProducer.TOPIC_SKIN_ANALYSIS_COMPLETED)
                .payload("{\"sessionId\":\"s1\",\"userId\":1,\"detectedSkinType\":\"OILY\",\"concerns\":[]}")
                .published(false)
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void marksPublishedAfterBrokerAck() {
        OutboxEvent e = pendingEvent();
        SendResult<String, Object> sendResult = mock(SendResult.class);
        when(outboxRepository.findByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(e));
        when(eventProducer.publishSkinAnalysisCompleted(any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        publisher.publishPending();

        assertThat(e.getPublished()).isTrue();
        verify(outboxRepository).save(e);
    }

    @Test
    void leavesUnpublishedAndDoesNotSaveWhenSendFails() {
        OutboxEvent e = pendingEvent();
        when(outboxRepository.findByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(e));
        CompletableFuture<SendResult<String, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker down"));
        when(eventProducer.publishSkinAnalysisCompleted(any())).thenReturn(failed);

        publisher.publishPending();

        assertThat(e.getPublished()).isFalse();
        verify(outboxRepository, never()).save(any());
    }
}
