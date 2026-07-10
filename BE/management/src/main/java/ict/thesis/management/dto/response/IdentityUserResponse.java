package ict.thesis.management.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class IdentityUserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private boolean verified;
    private boolean active;

}
