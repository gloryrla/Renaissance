package econo.project1.vote;

import econo.project1.common.Cuisine;

import java.util.List;
import java.util.Set;

/**
 * GET /api/votes/{voteId} 응답. 현재 진행 상태 전체를 담는다.
 */
public record VoteDetailResponse(
        Long voteId,
        String title,
        VoteStatus status,
        List<CandidateMenuResponse> candidates,
        CandidateMenuResponse resultMenu,   // 확정 전엔 null
        boolean relaxed,
        Set<Cuisine> relaxedCuisines,
        boolean impossible
) {
}
