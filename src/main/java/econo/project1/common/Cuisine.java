package econo.project1.common;

import lombok.Getter;

/**
 * 8가지 종류, 1단게 카테고리 비선호 필터의 기준
 */

@Getter
public enum Cuisine {

    FASTFOOD("패스트푸두"),
    KOREAN("한식"),
    STEW_SOUP("찜 - 탕"),
    MEAT("고기"),
    ASIAN("아시안"),
    CHINESE("중식"),
    WESTERN("양식"),
    JAPANESE("일식");

    private final String label;

    Cuisine(String label) {
        this.label = label;
    }
}