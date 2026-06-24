package econo.project1.vote;

import econo.project1.auth.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 상태를 가진 투표 진행 API (②~⑤).
 * 투표 생성(①)은 VoteController(POST /api/groups/{groupId}/votes) 사용.
 *
 * 행위자(memberId)는 Authorization 헤더의 JWT 에서 가져온다(@LoginMember).
 */
@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteSessionController {

    private final VoteSessionService voteSessionService;

    // ② 그룹원 선호 제출
    @PostMapping("/{voteId}/preferences")
    public void submitPreference(@LoginMember Long memberId,
                                 @PathVariable Long voteId,
                                 @RequestBody MemberPreferenceRequest request) {
        voteSessionService.submitPreference(voteId, memberId, request);
    }

    // ③ 추천 실행 -> 후보 3개 확정
    @PostMapping("/{voteId}/recommend")
    public RecommendResultResponse recommend(@LoginMember Long memberId, @PathVariable Long voteId) {
        return voteSessionService.recommend(voteId, memberId);
    }

    // ④ 후보에 호/불호 투표
    @PostMapping("/{voteId}/ballots")
    public void submitBallot(@LoginMember Long memberId,
                             @PathVariable Long voteId,
                             @RequestBody BallotRequest request) {
        voteSessionService.submitBallot(voteId, memberId, request);
    }

    // ⑤ 집계 -> 최종 메뉴 확정
    @PostMapping("/{voteId}/result")
    public CandidateMenuResponse decide(@LoginMember Long memberId, @PathVariable Long voteId) {
        return voteSessionService.decide(voteId, memberId);
    }

    // 현재 진행 상태/후보/결과 조회
    @GetMapping("/{voteId}")
    public VoteDetailResponse get(@LoginMember Long memberId, @PathVariable Long voteId) {
        return voteSessionService.getDetail(voteId, memberId);
    }

    // 아직 선호를 안 낸 그룹원 목록
    @GetMapping("/{voteId}/pending-members")
    public List<MemberSummaryResponse> pendingMembers(@LoginMember Long memberId, @PathVariable Long voteId) {
        return voteSessionService.pendingMembers(voteId, memberId);
    }
}
