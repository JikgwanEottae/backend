package yagu.yagu.tourapi;

public enum Stadium {
    LG_TWINS(11, 11710),
    DOOSAN(11, 11710),
    KIWOOM(11, 11530),
    SSG(28, 28177),
    KT(41, 41111),
    SAMSUNG(27, 27260),
    HANHWA(30, 30140),
    LOTTE(26, 26260),
    NC(48, 48127),
    KIA(29, 29170);

    private final int areaCd;
    private final int signguCd;

    Stadium(int areaCd, int signguCd) {
        this.areaCd = areaCd;
        this.signguCd = signguCd;
    }
    public int getAreaCd() { return areaCd; }
    public int getSignguCd() { return signguCd; }
}
