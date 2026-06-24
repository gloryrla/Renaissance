package econo.project1.common;

import lombok.Getter;

/**
 * 음식 알레르겐(알레르기 유발 성분). 추천 1단계 하드필터 기준.
 * 코드값은 메뉴 데이터셋(menu-data.csv)의 알레르겐코드와 1:1로 일치한다.
 */
@Getter
public enum Restriction {
    EGG("계란"),
    MILK("우유"),
    WHEAT("밀"),
    SOY("대두"),
    NUT("견과"),
    CRUSTACEAN("갑각류"),
    SEAFOOD("해산물");

    private final String label;

    Restriction(String label) {
        this.label = label;
    }
}
