package econo.project1.vote;

import java.time.LocalDateTime;

/**
 * 투표 생성 요청.
 */
public record VoteCreateRequest(
        String title,
        LocalDateTime deadline
) {
}
