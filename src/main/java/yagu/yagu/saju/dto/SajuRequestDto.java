package yagu.yagu.saju.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SajuRequestDto {
    @JsonProperty("birth_date")
    private String birthDate;
    private String gender;
    @JsonProperty("team_name")
    private String teamName;
    @JsonProperty("timezone_offset")
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