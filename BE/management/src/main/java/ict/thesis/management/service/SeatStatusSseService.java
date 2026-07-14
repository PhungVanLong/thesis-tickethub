package ict.thesis.management.service;

import ict.thesis.management.dto.event.SeatStatusUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class SeatStatusSseService {

    private final Map<Long, List<SseEmitter>> eventEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long eventId) {
        SseEmitter emitter = new SseEmitter(180000L); // 3 minutes timeout

        eventEmitters.computeIfAbsent(eventId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(eventId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            removeEmitter(eventId, emitter);
        });
        emitter.onError(e -> {
            log.debug("SSE Emitter error for event {}: {}", eventId, e.getMessage());
            removeEmitter(eventId, emitter);
        });

        // Send initialization success message
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected to event " + eventId + " seat map stream"));
        } catch (IOException e) {
            try {
                emitter.complete();
            } catch (Exception ex) {}
            removeEmitter(eventId, emitter);
        }

        log.info("Client subscribed to seat updates for event: {}, total subscribers: {}", 
                 eventId, eventEmitters.get(eventId).size());
        return emitter;
    }

    public void broadcast(Long eventId, SeatStatusUpdateEvent event) {
        List<SseEmitter> emitters = eventEmitters.get(eventId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        log.info("Broadcasting seat status update for event {} to {} subscribers", eventId, emitters.size());
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("SEAT_UPDATE").data(event));
            } catch (IOException | IllegalStateException e) {
                log.info("Client disconnected from seat map stream ({}). Removing emitter.", e.getMessage());
                deadEmitters.add(emitter);
                try {
                    emitter.complete();
                } catch (Exception ex) {}
            }
        }
        
        for (SseEmitter dead : deadEmitters) {
            removeEmitter(eventId, dead);
        }
    }

    private void removeEmitter(Long eventId, SseEmitter emitter) {
        List<SseEmitter> emitters = eventEmitters.get(eventId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                eventEmitters.remove(eventId);
            }
        }
    }

    /**
     * Send periodic heartbeat (ping) to keep SSE connections alive 
     * and clean up disconnected clients promptly.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 20000)
    public void sendHeartbeat() {
        if (eventEmitters.isEmpty()) {
            return;
        }

        for (Map.Entry<Long, List<SseEmitter>> entry : eventEmitters.entrySet()) {
            Long eventId = entry.getKey();
            List<SseEmitter> emitters = entry.getValue();
            List<SseEmitter> deadEmitters = new java.util.ArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("HEARTBEAT").data("ping"));
                } catch (IOException | IllegalStateException e) {
                    deadEmitters.add(emitter);
                    try {
                        emitter.complete();
                    } catch (Exception ex) {}
                }
            }

            for (SseEmitter dead : deadEmitters) {
                removeEmitter(eventId, dead);
            }
        }
    }
}
