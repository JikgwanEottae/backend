package yagu.yagu.saju.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SajuRequestDto {

    /** YYYYMMDD (예: 19900515) */
    @NotBlank(message = "birth_date는 필수입니다")
    @JsonProperty("birth_date")
    private String birthDate;

    @NotBlank(message = "gender는 필수입니다")
    private String gender;

    /** 허용: doosan|kiwoom|samsung|lotte|kia|hanwha|ssg|nc|lg|kt */
    @NotBlank(message = "team_name은 필수입니다")
    @JsonProperty("team_name")
    private String teamName;

    /** 시간: 모르면 null, 알면 0~23 */
    private Integer time;

    // getters/setters
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

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }
}