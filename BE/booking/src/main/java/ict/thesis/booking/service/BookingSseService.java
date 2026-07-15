package ict.thesis.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class BookingSseService {

    // Store SseEmitter for each request ID
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // Cache for completed results if the client hasn't subscribed yet
    private final Map<String, Long> completedBookings = new ConcurrentHashMap<>();
    private final Map<String, String> failedBookings = new ConcurrentHashMap<>();

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
            log.info("SseEmitter not found for requestId: {}, caching success result.", requestId);
            completedBookings.put(requestId, orderId);
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
        } else {
            log.info("SseEmitter not found for requestId: {}, caching failed result.", requestId);
            failedBookings.put(requestId, reason);
        }
    }

    public SseEmitter subscribeToBookingResult(String requestId) {
        SseEmitter emitter = new SseEmitter(60000L); // 60 seconds timeout

        // Check if there is already a cached success result
        if (completedBookings.containsKey(requestId)) {
            Long orderId = completedBookings.remove(requestId);
            try {
                emitter.send(SseEmitter.event().name("SUCCESS").data(orderId));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // Check if there is already a cached failed result
        if (failedBookings.containsKey(requestId)) {
            String reason = failedBookings.remove(requestId);
            try {
                emitter.send(SseEmitter.event().name("FAILED").data(reason));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

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
}
