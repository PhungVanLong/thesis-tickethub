package ict.thesis.booking.service;

import ict.thesis.booking.enties.OrderItem;
import ict.thesis.booking.enties.Ticket;
import ict.thesis.booking.enties.enums.TicketStatus;
import ict.thesis.booking.repository.TicketRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;

    @Transactional
    public List<Ticket> issueTickets(List<OrderItem> orderItems, OffsetDateTime now) {
        List<Ticket> tickets = new ArrayList<>();
        OffsetDateTime issuedAt = now == null ? OffsetDateTime.now(ZoneOffset.UTC) : now;
        if (orderItems == null) {
            return tickets;
        }
        for (OrderItem orderItem : orderItems) {
            tickets.add(ticketRepository.save(Ticket.builder()
                    .orderItem(orderItem)
                    .seat(orderItem.getSeat())
                    .ticketCode(generateTicketCode())
                    .qrCodeUrl("https://booking.local/tickets/" + UUID.randomUUID())
                    .status(TicketStatus.ACTIVE)
                    .issuedAt(issuedAt)
                    .build()));
        }
        return tickets;
    }

    public List<Ticket> getTicketsByOrderId(Long orderId) {
        return ticketRepository.findByOrderItem_Order_Id(orderId);
    }

    private String generateTicketCode() {
        return "TCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}

