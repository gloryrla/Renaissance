package econo.project1.common;

import lombok.Getter;

/**
 * 지원 장소(고정 셋). 전남대학교 단과대학별 식당 검색 중심 좌표(x=경도, y=위도)와 반경(m)을 가진다.
 * 투표 생성 시 장소를 지정하면, 그 투표의 주변 식당 검색이 해당 단과대 좌표 기준으로 동작한다.
 *
 * 좌표는 전남대 광주 캠퍼스 단과대 건물 기준 근사값 — 정확한 위치가 필요하면 카카오맵에서 확인해 조정.
 */
@Getter
public enum School {

    // displayName, x(경도), y(위도), radius(m)
    GONGDAE("전남대 공과대학", 126.9100, 35.1780, 1500),
    YEDAE("전남대 예술대학", 126.9110, 35.1745, 1500),
    SANGDAE("전남대 경영대학", 126.9060, 35.1735, 1500);

    private final String displayName;
    private final double x;
    private final double y;
    private final int radius;

    School(String displayName, double x, double y, int radius) {
        this.displayName = displayName;
        this.x = x;
        this.y = y;
        this.radius = radius;
    }
}
