package yagu.yagu.saju.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SajuRequestDto {
    @NotBlank(message = "birth_date는 필수입니다")
    @JsonProperty("birth_date")
    private String birthDate;

    @NotBlank(message = "gender는 필수입니다")
    private String gender;

    @NotBlank(message = "team_name은 필수입니다")
    @JsonProperty("team_name")
    private String teamName;

    @JsonProperty(value = "timezone_offset", access = JsonProperty.Access.READ_ONLY)
    private Integer timezoneOffset;

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Integer getTimezoneOffset() {
        return timezoneOffset;
    }

    public void setTimezoneOffset(Integer timezoneOffset) {
        this.timezoneOffset = timezoneOffset;
    }
}