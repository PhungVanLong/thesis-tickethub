package ict.thesis.management.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrganizationRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 50, message = "Abbreviation name must not exceed 50 characters")
    private String abbreviationName;

    @Size(max = 13, message = "Tax code must not exceed 13 characters")
    @Pattern(regexp = "^[0-9]{10}$|^[0-9]{13}$", message = "Tax code must be 10 or 13 numeric digits")
    private String taxCode;

    @Size(max = 255, message = "Representative name must not exceed 255 characters")
    private String representativeName;

    @Size(max = 255, message = "Representative position must not exceed 255 characters")
    private String representativePosition;

    @Size(max = 15, message = "Hotline must not exceed 15 characters")
    @Pattern(regexp = "^[0-9]{9,15}$", message = "Hotline must be numeric and between 9 to 15 digits")
    private String hotline;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Official email must not exceed 255 characters")
    private String officialEmail;

    @Size(max = 100, message = "Province or city must not exceed 100 characters")
    private String provinceCity;

    @Size(max = 100, message = "District must not exceed 100 characters")
    private String district;

    @Size(max = 100, message = "Ward or commune must not exceed 100 characters")
    private String wardCommune;

    @Size(max = 1000, message = "Headquarter address must not exceed 1000 characters")
    private String headquarterAddress;

    @Size(max = 255, message = "Website URL must not exceed 255 characters")
    private String websiteUrl;

    @Size(max = 255, message = "Fanpage URL must not exceed 255 characters")
    private String fanpageUrl;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;
}
