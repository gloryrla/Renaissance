package econo.project1.vote;

/**
 * 투표 진행 단계.
 * RECOMMENDING: 그룹원 선호 수집 중(②) -> 추천 가능
 * VOTING: 후보 3개가 정해져 호/불호 투표 중(④)
 * CLOSED: 집계 완료, 최종 메뉴 확정(⑤)
 */
public enum VoteStatus {
    RECOMMENDING,
    VOTING,
    CLOSED
}
