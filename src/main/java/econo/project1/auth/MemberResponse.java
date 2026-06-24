package econo.project1.auth;

import econo.project1.member.Member;

/** 로그인 회원 요약. */
public record MemberResponse(Long id, String name) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getName());
    }
}
