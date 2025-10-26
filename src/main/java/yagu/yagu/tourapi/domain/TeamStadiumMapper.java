package yagu.yagu.tourapi.domain;

import java.util.HashMap;
import java.util.Map;

public class TeamStadiumMapper {

    private static final Map<String, String> TEAM_TO_STADIUM = new HashMap<>();

    static {
        TEAM_TO_STADIUM.put("samsung", "대구삼성라이온즈파크");
        TEAM_TO_STADIUM.put("hanwha", "대전한화생명볼파크");
        TEAM_TO_STADIUM.put("nc", "마산야구장");
        TEAM_TO_STADIUM.put("kiwoom", "고척스카이돔");
        TEAM_TO_STADIUM.put("ssg", "인천SSG랜더스필드");
        TEAM_TO_STADIUM.put("kt", "수원KT위즈파크");
        TEAM_TO_STADIUM.put("lotte", "사직야구장");
        TEAM_TO_STADIUM.put("doosan", "잠실야구장");
        TEAM_TO_STADIUM.put("lg", "잠실야구장");
        TEAM_TO_STADIUM.put("kia", "광주기아챔피언스필드");
    }

    public static String getStadiumByTeam(String team) {
        String stadium = TEAM_TO_STADIUM.get(team);
        if (stadium == null) {
            throw new IllegalArgumentException("존재하지 않는 팀명입니다: " + team);
        }
        return stadium;
    }

    public static boolean isValidTeam(String team) {
        return TEAM_TO_STADIUM.containsKey(team);
    }
}
