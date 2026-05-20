package com.lakeserl.payment_service.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.lakeserl.payment_service.models.entity.OutboxEvent;
import com.lakeserl.payment_service.models.enums.OutboxStatus;
import com.lakeserl.payment_service.repository.OutboxRepository;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxEventPublisher outboxEventPublisher;

    @Test
    @SuppressWarnings("unchecked")
    void testPublishPendingEvents_Success() {
        // Arrange
        OutboxEvent event = OutboxEvent.builder()
                .id(1L)
                .aggregateType("Payment")
                .aggregateId("999")
                .eventType("payment.completed")
                .payload("{\"orderId\":\"999\"}")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("payment.completed"), eq("999"), eq("{\"orderId\":\"999\"}")))
                .thenReturn(future);

        // Act
        outboxEventPublisher.publishPendingEvents();

        // Assert
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEvent processed = captor.getValue();

        assertEquals(OutboxStatus.SENT, processed.getStatus());
        assertNotNull(processed.getProcessedAt());
    }

    @Test
    void testPublishPendingEvents_NoPending() {
        // Arrange
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        // Act
        outboxEventPublisher.publishPendingEvents();

        // Assert
        verifyNoInteractions(kafkaTemplate);
        verify(outboxRepository, never()).save(any());
    }
}
