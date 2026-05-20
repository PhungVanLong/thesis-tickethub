package ict.thesis.management.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdentityUserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private boolean verified;
    private boolean active;

}
