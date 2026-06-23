package econo.project1.vote;

/** 응답용 회원 요약. */
public record MemberSummaryResponse(
        Long memberId,
        String name
) {
}
