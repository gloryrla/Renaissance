package econo.project1.vote;

import econo.project1.auth.LoginMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 상태를 가진 투표 진행 API (②~⑤).
 * 투표 생성(①)은 VoteController(POST /api/groups/{groupId}/votes) 사용.
 *
 * 행위자(memberId)는 Authorization 헤더의 JWT 에서 가져온다(@LoginMember).
 */

@Tag(name = "Vote", description = "투표 생성, 실행, 삭제, 조회 API")
@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteSessionController {

    private final VoteSessionService voteSessionService;

    // ② 그룹원 선호 제출
    @Operation(
            summary = "그룹원 선호 제출",
            description = "각 그룹원 마다 선호(투표)를 제출하는 엔드포인트"
    )
    @PostMapping("/{voteId}/preferences")
    public void submitPreference(@LoginMember Long memberId,
                                 @PathVariable Long voteId,
                                 @RequestBody MemberPreferenceRequest request) {
        voteSessionService.submitPreference(voteId, memberId, request);
    }

    // ③ 추천 실행 -> 후보 3개 확정
    @Operation(
            summary = "추천 실행",
            description = "그룹메뉴 추천을 실행하는 엔드포인트"
    )
    @PostMapping("/{voteId}/recommend")
    public RecommendResultResponse recommend(@LoginMember Long memberId, @PathVariable Long voteId) {
        return voteSessionService.recommend(voteId, memberId);
    }

    // ④ 후보에 호/불호 투표. 그룹원 전원이 제출하면 ⑤ 집계가 자동 실행되어 CLOSED 로 마감된다.
    @Operation(
            summary = "호/불호 투표 제출",
            description = "확정된 후보에 호/불호 투표를 제출하는 엔드포인트. 그룹원 전원이 제출하면 집계가 자동 실행되어 마감된다."
    )
    @PostMapping("/{voteId}/ballots")
    public void submitBallot(@LoginMember Long memberId,
                             @PathVariable Long voteId,
                             @RequestBody BallotRequest request) {
        voteSessionService.submitBallot(voteId, memberId, request);
    }

    // ⑤ 방장 강제 마감: 전원이 투표하지 않았어도 모인 표로 즉시 집계·마감 (OWNER 만 가능)
    @Operation(
            summary = "방장 강제 마감",
            description = "전원이 투표하지 않았어도 모인 표로 즉시 집계·마감하는 엔드포인트 (방장 OWNER 만 가능)"
    )
    @PostMapping("/{voteId}/close")
    public CandidateMenuResponse close(@LoginMember Long memberId, @PathVariable Long voteId) {
        return voteSessionService.forceClose(voteId, memberId);
    }

    // 현재 진행 상태/후보/결과 조회 (집계 완료 시 resultMenu 로 최종 메뉴 확인)
    @Operation(
            summary = "투표 진행 상태 조회",
            description = "현재 진행 상태/후보/결과를 조회하는 엔드포인트. 집계 완료 시 resultMenu 로 최종 메뉴를 확인한다."
    )
    @GetMapping("/{voteId}")
    public VoteDetailResponse get(@LoginMember Long memberId, @PathVariable Long voteId) {
        return voteSessionService.getDetail(voteId, memberId);
    }

    // 아직 선호를 안 낸 그룹원 목록
    @Operation(
            summary = "선호 미제출 그룹원 조회",
            description = "아직 선호를 제출하지 않은 그룹원 목록을 조회하는 엔드포인트"
    )
    @GetMapping("/{voteId}/pending-members")
    public List<MemberSummaryResponse> pendingMembers(@LoginMember Long memberId, @PathVariable Long voteId) {
        return voteSessionService.pendingMembers(voteId, memberId);
    }
}
