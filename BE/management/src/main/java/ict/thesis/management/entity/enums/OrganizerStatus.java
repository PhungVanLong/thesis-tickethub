package ict.thesis.management.entity.enums;

public enum OrganizerStatus {
    PENDING,    // Mới đăng ký, đang chờ Admin Việt Nam kiểm tra Mã số thuế/Hồ sơ
    APPROVED,   // Đã kiểm duyệt thành công, hoạt động hợp pháp
    REJECTED,   // Bị từ chối (do sai mã số thuế, giả mạo thông tin...)
    SUSPENDED   // Bị tạm khóa (do vi phạm quy chế, gian lận vé...)
}
