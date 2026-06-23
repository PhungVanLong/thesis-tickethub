package ict.thesis.management.security;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserContext {
    private Long userId;
    private String role;
    private String email;
}
