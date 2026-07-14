package ict.thesis.booking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ict.thesis.booking.enties.EventRef;
import ict.thesis.booking.enties.TicketTierRef;
import ict.thesis.booking.repository.EventRefRepository;
import ict.thesis.booking.repository.TicketTierRefRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventSyncConsumer {

    private final EventRefRepository eventRefRepository;
    private final TicketTierRefRepository ticketTierRefRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "${kafka.topic.event-published}", groupId = "booking-group")
    @Transactional
    public void consumeEventPublished(String payload) {
        log.info("Received event published sync: {}", payload);
        try {
            JsonNode root = objectMapper.readTree(payload);
            Long eventId = root.get("eventId").asLong();
            String title = root.get("title").asText();
            String venue = root.has("venue") && !root.get("venue").isNull() ? root.get("venue").asText() : null;
            String city = root.has("city") && !root.get("city").isNull() ? root.get("city").asText() : null;
            String bannerUrl = root.has("bannerUrl") && !root.get("bannerUrl").isNull() ? root.get("bannerUrl").asText() : null;
            
            Instant startTime = root.has("startTime") && !root.get("startTime").isNull() ? Instant.parse(root.get("startTime").asText()) : null;
            Instant endTime = root.has("endTime") && !root.get("endTime").isNull() ? Instant.parse(root.get("endTime").asText()) : null;

            EventRef eventRef = EventRef.builder()
                    .id(eventId)
                    .title(title)
                    .startTime(startTime)
                    .endTime(endTime)
                    .venue(venue)
                    .city(city)
                    .bannerUrl(bannerUrl)
                    .syncedAt(Instant.now())
                    .build();

            eventRefRepository.save(eventRef);
            log.info("Successfully synced EventRef: {} ({})", eventId, title);

            if (root.has("ticketTiers") && root.get("ticketTiers").isArray()) {
                JsonNode tiers = root.get("ticketTiers");
                for (JsonNode tier : tiers) {
                    Long tierId = tier.get("id").asLong();
                    String name = tier.get("name").asText();
                    BigDecimal price = new BigDecimal(tier.get("price").asText());
                    Integer quantityAvailable = tier.has("quantityAvailable") && !tier.get("quantityAvailable").isNull() 
                            ? tier.get("quantityAvailable").asInt() : null;

                    TicketTierRef tierRef = TicketTierRef.builder()
                            .id(tierId)
                            .eventId(eventId)
                            .eventName(title)
                            .name(name)
                            .price(price)
                            .quantityAvailable(quantityAvailable)
                            .syncedAt(Instant.now())
                            .build();

                    ticketTierRefRepository.save(tierRef);
                    log.info("Successfully synced TicketTierRef: {} ({})", tierId, name);
                }
            }

        } catch (Exception e) {
            log.error("Failed to process event published sync message", e);
        }
    }
}
