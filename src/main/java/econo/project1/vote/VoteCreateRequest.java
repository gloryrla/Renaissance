package econo.project1.vote;

import econo.project1.common.School;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 투표 생성 요청.
 * school: 식당 검색 기준이 되는 학교(장소). 필수.
 * participantMemberIds: 이 투표에 참여할 그룹원 id 목록. 최소 1명 필수(방장은 자동 포함).
 */
public record VoteCreateRequest(
        String title,
        LocalDateTime deadline,
        School school,
        Set<Long> participantMemberIds
) {
}
