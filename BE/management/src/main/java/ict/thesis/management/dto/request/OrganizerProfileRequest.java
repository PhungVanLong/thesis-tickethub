package ict.thesis.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OrganizerProfileRequest {
    @NotNull(message = "userId is required")
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

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

    public String getAbbreviationName() { return abbreviationName; }
    public void setAbbreviationName(String abbreviationName) { this.abbreviationName = abbreviationName; }

    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }

    public String getRepresentativeName() { return representativeName; }
    public void setRepresentativeName(String representativeName) { this.representativeName = representativeName; }

    public String getRepresentativePosition() { return representativePosition; }
    public void setRepresentativePosition(String representativePosition) { this.representativePosition = representativePosition; }

    public String getHotline() { return hotline; }
    public void setHotline(String hotline) { this.hotline = hotline; }

    public String getOfficialEmail() { return officialEmail; }
    public void setOfficialEmail(String officialEmail) { this.officialEmail = officialEmail; }

    public String getProvinceCity() { return provinceCity; }
    public void setProvinceCity(String provinceCity) { this.provinceCity = provinceCity; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getWardCommune() { return wardCommune; }
    public void setWardCommune(String wardCommune) { this.wardCommune = wardCommune; }

    public String getHeadquarterAddress() { return headquarterAddress; }
    public void setHeadquarterAddress(String headquarterAddress) { this.headquarterAddress = headquarterAddress; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public String getFanpageUrl() { return fanpageUrl; }
    public void setFanpageUrl(String fanpageUrl) { this.fanpageUrl = fanpageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

