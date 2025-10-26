package yagu.yagu.saju.dto;

import java.util.Arrays;

public enum TeamName {
    DOOSAN("doosan", "두산 베어스"),
    KIWOOM("kiwoom", "키움 히어로즈"),
    SAMSUNG("samsung", "삼성 라이온즈"),
    LOTTE("lotte", "롯데 자이언츠"),
    KIA("kia", "기아 타이거즈"),
    HANWHA("hanwha", "한화 이글스"),
    SSG("ssg", "SSG 랜더스"),
    NC("nc", "NC 다이노스"),
    LG("lg", "LG 트윈스"),
    KT("kt", "KT 위즈");

    private final String clientName;
    private final String standardName;

    TeamName(String clientName, String standardName) {
        this.clientName = clientName;
        this.standardName = standardName;
    }

    public String clientName() {
        return clientName;
    }

    public String standardName() {
        return standardName;
    }

    public String englishName() {
        return name().toLowerCase();
    }

    public static String toStandardOrThrow(String clientName) {
        return Arrays.stream(values())
                .filter(t -> t.clientName.equals(clientName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("허용되지 않은 team_name: " + clientName)).standardName;
    }

    public static String toEnglishOrThrow(String clientName) {
        return Arrays.stream(values())
                .filter(t -> t.clientName.equals(clientName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("허용되지 않은 team_name: " + clientName))
                .englishName();
    }
}