package econo.project1.vote;

import econo.project1.common.UnauthorizedException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 상태를 가진 투표 진행 API (②~⑤).
 * 투표 생성(①)은 기존 VoteController(POST /api/groups/{groupId}/votes)를 그대로 사용한다.
 *
 * 행위자(memberId)는 요청 바디가 아니라 로그인 세션에서 가져온다(도용 방지).
 */
@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteSessionController {

    private final VoteSessionService voteSessionService;

    // ② 그룹원 선호 제출
    @PostMapping("/{voteId}/preferences")
    public void submitPreference(@PathVariable Long voteId,
                                 @RequestBody MemberPreferenceRequest request,
                                 HttpSession session) {
        voteSessionService.submitPreference(voteId, loginMemberId(session), request);
    }

    // ③ 추천 실행 -> 후보 3개 확정
    @PostMapping("/{voteId}/recommend")
    public RecommendResultResponse recommend(@PathVariable Long voteId, HttpSession session) {
        return voteSessionService.recommend(voteId, loginMemberId(session));
    }

    // ④ 후보에 호/불호 투표
    @PostMapping("/{voteId}/ballots")
    public void submitBallot(@PathVariable Long voteId,
                             @RequestBody BallotRequest request,
                             HttpSession session) {
        voteSessionService.submitBallot(voteId, loginMemberId(session), request);
    }

    // ⑤ 집계 -> 최종 메뉴 확정
    @PostMapping("/{voteId}/result")
    public CandidateMenuResponse decide(@PathVariable Long voteId, HttpSession session) {
        return voteSessionService.decide(voteId, loginMemberId(session));
    }

    // 현재 진행 상태/후보/결과 조회
    @GetMapping("/{voteId}")
    public VoteDetailResponse get(@PathVariable Long voteId, HttpSession session) {
        return voteSessionService.getDetail(voteId, loginMemberId(session));
    }

    // 아직 선호를 안 낸 그룹원 목록
    @GetMapping("/{voteId}/pending-members")
    public List<MemberSummaryResponse> pendingMembers(@PathVariable Long voteId, HttpSession session) {
        return voteSessionService.pendingMembers(voteId, loginMemberId(session));
    }

    // 세션에서 로그인 회원 id를 꺼낸다. 없으면 401.
    private Long loginMemberId(HttpSession session) {
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        return memberId;
    }
}
