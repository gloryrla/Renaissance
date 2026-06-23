package econo.project1.vote;

import econo.project1.common.Cuisine;
import econo.project1.common.Restriction;
import econo.project1.member.Member;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * ② 그룹원 한 명이 한 투표에 제출한 선호.
 * (vote, member)당 한 행. 다시 제출하면 이 행을 덮어쓴다.
 */
@Entity
@Getter
@Setter
public class VotePreference {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id")
    private Vote vote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 오늘 안 당기는 카테고리 -> 추천 2단계 필터
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "vote_pref_disliked_cuisine", joinColumns = @JoinColumn(name = "preference_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "cuisine")
    private Set<Cuisine> dislikedCuisines = new HashSet<>();

    // 알레르기/식이 제한 -> 추천 1단계 하드필터
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "vote_pref_restriction", joinColumns = @JoinColumn(name = "preference_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "restriction")
    private Set<Restriction> restrictions = new HashSet<>();
}
