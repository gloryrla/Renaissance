package econo.project1.vote;

import java.util.Map;

/**
 * ④ 호/불호 투표 요청.
 * memberId: 투표하는 그룹원.
 * choices: menuId -> LIKE/DISLIKE (후보 메뉴에 대해서만).
 */
public record BallotRequest(
        Long memberId,
        Map<Long, VoteService.Choice> choices
) {
}
