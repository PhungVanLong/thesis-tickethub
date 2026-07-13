package ict.thesis.management.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerificationEmail(String toEmail, String orgName, boolean isApproved, String reason) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Cannot send verification email: recipient address is empty.");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("longberray88@gmail.com");
            message.setTo(toEmail);
            
            String subject = "[TicketHub] Kết quả duyệt đăng ký Tổ chức: " + orgName;
            message.setSubject(subject);

            String statusText = isApproved ? "ĐÃ ĐƯỢC PHÊ DUYỆT" : "BỊ TỪ CHỐI";
            StringBuilder body = new StringBuilder();
            body.append("Xin chào ban quản trị tổ chức ").append(orgName).append(",\n\n");
            body.append("Yêu cầu đăng ký thông tin tổ chức của bạn trên hệ thống TicketHub đã ").append(statusText).append(".\n");
            
            if (isApproved) {
                body.append("Bây giờ bạn đã có thể đăng nhập bằng tài khoản OWNER để tiến hành tạo và quản lý sự kiện.\n");
            } else {
                body.append("Lý do từ chối: ").append(reason).append("\n");
                body.append("Vui lòng kiểm tra lại thông tin và gửi lại yêu cầu đăng ký nếu cần.\n");
            }
            
            body.append("\nTrân trọng,\nBan quản trị TicketHub");
            message.setText(body.toString());

            log.info("Sending email to organization: {} ({}), status: {}", orgName, toEmail, statusText);
            mailSender.send(message);
            log.info("Email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}", toEmail, e);
        }
    }

    @Async
    public void sendTicketPurchaseSuccessEmail(
            String toEmail,
            String eventTitle,
            String eventVenue,
            String eventDate,
            String orderCode,
            java.math.BigDecimal totalAmount,
            java.util.List<java.util.Map<String, Object>> tickets) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Cannot send ticket purchase success email: recipient address is empty.");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("longberray88@gmail.com");
            message.setTo(toEmail);

            String subject = "[TicketHub] Xác nhận đặt vé thành công: " + eventTitle;
            message.setSubject(subject);

            StringBuilder body = new StringBuilder();
            body.append("Xin chào quý khách,\n\n");
            body.append("Chúc mừng bạn đã đặt vé thành công trên hệ thống TicketHub!\n\n");
            body.append("--- THÔNG TIN ĐƠN HÀNG ---\n");
            body.append("Mã đơn hàng: ").append(orderCode).append("\n");
            body.append("Sự kiện: ").append(eventTitle).append("\n");
            body.append("Địa điểm: ").append(eventVenue).append("\n");
            body.append("Thời gian: ").append(eventDate).append("\n");
            body.append("Tổng thanh toán: ").append(totalAmount).append(" VND\n\n");

            body.append("--- THÔNG TIN VÉ ---\n");
            for (int i = 0; i < tickets.size(); i++) {
                java.util.Map<String, Object> t = tickets.get(i);
                body.append("Vé số ").append(i + 1).append(":\n");
                body.append("  - Hạng vé: ").append(t.get("ticketTierName")).append("\n");
                body.append("  - Vị trí ghế: ").append(t.get("seatCode")).append("\n");
                body.append("  - Mã vé: ").append(t.get("ticketCode")).append("\n");
                body.append("  - Quét mã QR check-in tại đây: ").append(t.get("qrCodeUrl")).append("\n\n");
            }

            body.append("Cảm ơn bạn đã đồng hành cùng TicketHub. Hẹn gặp lại bạn tại sự kiện!\n\n");
            body.append("Trân trọng,\nBan quản trị TicketHub");
            message.setText(body.toString());

            log.info("Sending ticket success email to customer: {}", toEmail);
            mailSender.send(message);
            log.info("Ticket email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send ticket success email to {}", toEmail, e);
        }
    }
}
