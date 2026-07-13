package ict.thesis.booking.service;

import ict.thesis.booking.dto.BookingDtos.CreateBookingRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    // Store SseEmitter for each request ID
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private static final String BOOKING_TOPIC = "booking.requests";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingMessage {
        private String requestId;
        private CreateBookingRequest payload;
    }

    public String submitBookingRequest(CreateBookingRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Submitting booking request {} to Kafka", requestId);
        
        BookingMessage message = new BookingMessage(requestId, request);
        kafkaTemplate.send(BOOKING_TOPIC, requestId, message);
        return requestId;
    }

    public SseEmitter subscribeToBookingResult(String requestId) {
        SseEmitter emitter = new SseEmitter(60000L); // 60 seconds timeout
        emitters.put(requestId, emitter);

        emitter.onCompletion(() -> emitters.remove(requestId));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(requestId);
        });
        emitter.onError(e -> {
            emitter.completeWithError(e);
            emitters.remove(requestId);
        });
        
        // Send a dummy event to establish connection immediately
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void notifyBookingSuccess(String requestId, Long orderId) {
        SseEmitter emitter = emitters.get(requestId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("SUCCESS").data(orderId));
                emitter.complete();
            } catch (IOException e) {
                log.error("Failed to send SSE for requestId {}", requestId, e);
                emitter.completeWithError(e);
            } finally {
                emitters.remove(requestId);
            }
        } else {
            log.warn("SseEmitter not found for requestId: {}", requestId);
        }
    }
    
    public void notifyBookingFailed(String requestId, String reason) {
        SseEmitter emitter = emitters.get(requestId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("FAILED").data(reason));
                emitter.complete();
            } catch (IOException e) {
                log.error("Failed to send FAILED SSE for requestId {}", requestId, e);
                emitter.completeWithError(e);
            } finally {
                emitters.remove(requestId);
            }
        }
    }
}
