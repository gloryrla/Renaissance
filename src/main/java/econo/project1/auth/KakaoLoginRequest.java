package econo.project1.auth;

/** 프론트가 카카오에서 받은 인가코드를 전달. */
public record KakaoLoginRequest(String code) {
}
