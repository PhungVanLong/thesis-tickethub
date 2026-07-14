package ict.thesis.management.controller;

import ict.thesis.management.dto.request.SeatMapRequest;
import ict.thesis.management.dto.response.SeatMapResponse;
import ict.thesis.management.security.UserContextHolder;
import ict.thesis.management.service.SeatMapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/seat-maps")
@RequiredArgsConstructor
public class SeatMapController {

    private final SeatMapService seatMapService;
    private final ict.thesis.management.service.SeatStatusSseService seatStatusSseService;

    /**
     * GET /api/events/{eventId}/seat-maps
     * Lấy toàn bộ sơ đồ ghế của sự kiện.
     * Public - không cần auth (để FE hiển thị khi chọn ghế).
     */
    @GetMapping
    public ResponseEntity<List<SeatMapResponse>> getSeatMaps(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(seatMapService.getSeatMaps(eventId));
    }

    /**
     * GET /api/events/{eventId}/seat-maps/stream
     * Đăng ký nhận luồng dữ liệu thay đổi trạng thái ghế theo thời gian thực.
     */
    @GetMapping(path = "/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamSeatUpdates(
            @PathVariable Long eventId) {
        return seatStatusSseService.subscribe(eventId);
    }

    /**
     * PUT /api/events/{eventId}/seat-maps
     * Thêm hoặc thay thế toàn bộ sơ đồ ghế.
     * Chỉ OWNER của tổ chức có thể gọi.
     */
    @PutMapping
    public ResponseEntity<List<SeatMapResponse>> updateSeatMaps(
            @PathVariable Long eventId,
            @Valid @RequestBody List<SeatMapRequest> requests) {
        Long userId = UserContextHolder.getContext().getUserId();
        return ResponseEntity.ok(seatMapService.updateSeatMaps(userId, eventId, requests));
    }
}
