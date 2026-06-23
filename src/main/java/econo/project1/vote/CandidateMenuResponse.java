package econo.project1.vote;

import econo.project1.common.Cuisine;
import econo.project1.menu.Menu;

/**
 * 응답용 메뉴 요약. (엔티티를 그대로 직렬화하면 LAZY 로딩 문제가 생기므로 DTO로 변환)
 */
public record CandidateMenuResponse(
        Long menuId,
        String name,
        Cuisine cuisine
) {
    public static CandidateMenuResponse from(Menu menu) {
        return new CandidateMenuResponse(menu.getId(), menu.getName(), menu.getCuisine());
    }
}
