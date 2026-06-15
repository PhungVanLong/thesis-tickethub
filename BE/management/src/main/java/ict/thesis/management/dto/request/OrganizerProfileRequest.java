package ict.thesis.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrganizerProfileRequest {
    private Long userId;

    @NotBlank(message = "organizationName is required")
    private String organizationName;

    private String abbreviationName;
    private String taxCode;
    private String representativeName;
    private String representativePosition;
    private String hotline;
    private String officialEmail;
    private String provinceCity;
    private String district;
    private String wardCommune;
    private String headquarterAddress;
    private String websiteUrl;
    private String fanpageUrl;
    private String description;

}
