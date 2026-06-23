package econo.project1.vote;

import econo.project1.common.Cuisine;

import java.util.List;
import java.util.Set;

/**
 * ③ 추천 실행 결과 응답.
 */
public record RecommendResultResponse(
        List<CandidateMenuResponse> candidates,
        boolean relaxed,                 // 카테고리 완화가 일어났는가
        Set<Cuisine> relaxedCuisines,    // 완화로 되살린 카테고리
        boolean impossible               // 알레르기만으로 후보를 못 만든 경우
) {
}
