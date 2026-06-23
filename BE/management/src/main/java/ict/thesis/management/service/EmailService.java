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
}
