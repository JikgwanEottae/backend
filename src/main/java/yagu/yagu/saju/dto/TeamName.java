package yagu.yagu.saju.dto;

import java.util.Arrays;

public enum TeamName {
    DOOSAN("두산", "두산 베어스"),
    KIWOOM("키움", "키움 히어로즈"),
    SAMSUNG("삼성", "삼성 라이온즈"),
    LOTTE("롯데", "롯데 자이언츠"),
    KIA("KIA", "기아 타이거즈"),
    HANWHA("한화", "한화 이글스"),
    SSG("SSG", "SSG 랜더스"),
    NC("NC", "NC 다이노스"),
    LG("LG", "LG 트윈스"),
    KT("KT", "KT 위즈");

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