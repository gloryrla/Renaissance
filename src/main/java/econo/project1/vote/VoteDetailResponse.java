package econo.project1.vote;

import econo.project1.common.Cuisine;
import econo.project1.common.School;

import java.util.List;
import java.util.Set;

/**
 * GET /api/votes/{voteId} 응답. 현재 진행 상태 전체를 담는다.
 */
public record VoteDetailResponse(
        Long voteId,
        String title,
        VoteStatus status,
        School school,                      // 식당 검색 기준 학교
        List<CandidateMenuResponse> candidates,
        CandidateMenuResponse resultMenu,   // 확정 전엔 null
        boolean relaxed,
        Set<Cuisine> relaxedCuisines,
        boolean impossible
) {
}
