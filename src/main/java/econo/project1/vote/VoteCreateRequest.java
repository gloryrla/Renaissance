package econo.project1.vote;

import econo.project1.common.School;

import java.time.LocalDateTime;

/**
 * 투표 생성 요청.
 * school: 식당 검색 기준이 되는 학교(장소). 필수.
 */
public record VoteCreateRequest(
        String title,
        LocalDateTime deadline,
        School school
) {
}
