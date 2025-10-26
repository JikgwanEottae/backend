package yagu.yagu.saju.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SajuResponseDto {
    private double score;
    @JsonProperty("compatibility_type")
    private String compatibilityType;
    @JsonProperty("today_fortune")
    private String todayFortune;
    private String recommendation;
    @JsonProperty("time_note")
    private String timeNote;
    @JsonProperty("favoriteTeam")
    private String favoriteTeam;

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getCompatibilityType() {
        return compatibilityType;
    }

    public void setCompatibilityType(String compatibilityType) {
        this.compatibilityType = compatibilityType;
    }

    public String getTodayFortune() {
        return todayFortune;
    }

    public void setTodayFortune(String todayFortune) {
        this.todayFortune = todayFortune;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getTimeNote() {
        return timeNote;
    }

    public void setTimeNote(String timeNote) {
        this.timeNote = timeNote;
    }

    public String getFavoriteTeam() {
        return favoriteTeam;
    }

    public void setFavoriteTeam(String favoriteTeam) {
        this.favoriteTeam = favoriteTeam;
    }
}
