package yagu.yagu.tourapi;

public enum Region {
    SEOUL(1, null),
    SUWON(31, 1),
    INCHEON(2, null),
    DAEJEON(3, null),
    DAEGU(4, null),
    BUSAN(6, null),
    CHANGWON(38, 1),
    GWANGJU(5, null);

    private final int areaCode;
    private final Integer sigunguCode;

    Region(int areaCode, Integer sigunguCode) {
        this.areaCode = areaCode;
        this.sigunguCode = sigunguCode;
    }
    public int getAreaCode() { return areaCode; }
    public Integer getSigunguCode() { return sigunguCode; }
}
