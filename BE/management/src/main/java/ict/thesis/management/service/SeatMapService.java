package ict.thesis.management.service;

import ict.thesis.management.dto.request.SeatMapRequest;
import ict.thesis.management.dto.request.SeatRequest;
import ict.thesis.management.dto.response.SeatMapResponse;
import ict.thesis.management.dto.response.SeatResponse;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.OrganizationMember;
import ict.thesis.management.entity.Seat;
import ict.thesis.management.entity.SeatMap;
import ict.thesis.management.entity.TicketTier;
import ict.thesis.management.entity.enums.EventStatus;
import ict.thesis.management.entity.enums.OrganizationRole;
import ict.thesis.management.entity.enums.SeatStatus;
import ict.thesis.management.repository.EventsRepository;
import ict.thesis.management.repository.OrganizationMemberRepository;
import ict.thesis.management.repository.SeatMapRepository;
import ict.thesis.management.repository.SeatRepository;
import ict.thesis.management.repository.TicketTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SeatMapService {

    private final EventsRepository eventsRepository;
    private final SeatMapRepository seatMapRepository;
    private final SeatRepository seatRepository;
    private final TicketTierRepository ticketTierRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    /**
     * GET /api/events/{eventId}/seat-maps
     * Trả về toàn bộ sơ đồ ghế của sự kiện (bao gồm ghế + trạng thái).
     */
    @Transactional(readOnly = true)
    public List<SeatMapResponse> getSeatMaps(Long eventId) {
        eventsRepository.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        List<SeatMap> seatMaps = seatMapRepository.findByEventId(eventId);
        return seatMaps.stream().map(this::toSeatMapResponse).toList();
    }

    /**
     * PUT /api/events/{eventId}/seat-maps
     * Thêm hoặc thay thế toàn bộ sơ đồ ghế của sự kiện.
     * Chỉ OWNER của tổ chức có thể gọi API này.
     * Sự kiện phải ở trạng thái PENDING (chưa được duyệt).
     */
    @Transactional
    public List<SeatMapResponse> updateSeatMaps(Long userId, Long eventId, List<SeatMapRequest> requests) {
        Events event = eventsRepository.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        // Kiểm tra quyền
        OrganizationMember member = organizationMemberRepository
            .findByOrganizationIdAndUserId(event.getOrganization().getId(), userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN, "User is not a member of the organization"));

        if (member.getMemberRole() != OrganizationRole.OWNER) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Only the organization owner can update seat maps");
        }

        // Chỉ cho phép cập nhật khi sự kiện còn ở PENDING
        if (event.getStatus() == EventStatus.PUBLISHED || event.getStatus() == EventStatus.CANCELLED) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cannot update seat maps for a " + event.getStatus().name().toLowerCase() + " event");
        }

        // Xoá toàn bộ seat maps + seats cũ của event này
        List<SeatMap> existingSeatMaps = seatMapRepository.findByEventId(eventId);
        for (SeatMap sm : existingSeatMaps) {
            seatRepository.deleteAllBySeatMapId(sm.getId());
        }
        seatMapRepository.deleteAll(existingSeatMaps);

        // Lấy danh sách ticket tiers của event để map theo tên
        List<TicketTier> tiers = ticketTierRepository.findByEventId(eventId);
        Map<String, TicketTier> tierByName = new HashMap<>();
        for (TicketTier t : tiers) {
            tierByName.put(t.getName(), t);
        }

        Instant now = Instant.now();

        // Tạo mới seat maps và seats
        for (SeatMapRequest req : requests) {
            SeatMap seatMap = new SeatMap();
            seatMap.setEvent(event);
            seatMap.setName(req.getName());
            seatMap.setTotalRows(req.getTotalRows());
            seatMap.setTotalCols(req.getTotalCols());
            seatMap.setLayoutJson(req.getLayoutJson());
            seatMap.setCreatedAt(now);
            SeatMap savedMap = seatMapRepository.save(seatMap);

            if (req.getSeats() != null) {
                for (SeatRequest seatReq : req.getSeats()) {
                    TicketTier tier = tierByName.get(seatReq.getTicketTierName());
                    if (tier == null) {
                        throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Ticket tier '" + seatReq.getTicketTierName() + "' not found for this event");
                    }

                    Seat seat = new Seat();
                    seat.setSeatMap(savedMap);
                    seat.setTicketTier(tier);
                    seat.setSeatCode(seatReq.getSeatCode());
                    seat.setRowLabel(seatReq.getRowLabel());
                    seat.setColNumber(seatReq.getColNumber());
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seatRepository.save(seat);

                    // Gán seatMap vào tier nếu chưa có
                    if (tier.getSeatMap() == null) {
                        tier.setSeatMap(savedMap);
                        ticketTierRepository.save(tier);
                    }
                }
            }
        }

        return getSeatMaps(eventId);
    }

    // ── Helpers ──────────────────────────────────────────────

    private SeatMapResponse toSeatMapResponse(SeatMap sm) {
        List<Seat> seats = seatRepository.findBySeatMapId(sm.getId());
        List<SeatResponse> seatResponses = seats.stream()
            .map(s -> new SeatResponse(
                s.getId(),
                s.getSeatCode(),
                s.getRowLabel(),
                s.getColNumber(),
                s.getStatus(),
                s.getTicketTier() != null ? s.getTicketTier().getId() : null,
                s.getTicketTier() != null ? s.getTicketTier().getName() : null,
                s.getTicketTier() != null ? s.getTicketTier().getColorCode() : null
            ))
            .toList();

        return new SeatMapResponse(
            sm.getId(),
            sm.getEvent() != null ? sm.getEvent().getId() : null,
            sm.getName(),
            sm.getTotalRows(),
            sm.getTotalCols(),
            sm.getLayoutJson(),
            sm.getCreatedAt(),
            seatResponses
        );
    }
}
