package econo.project1.auth;

/** 로그인 성공 응답: JWT + 회원 정보. */
public record LoginResponse(String token, MemberResponse member) {
}
