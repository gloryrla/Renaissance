package econo.project1.auth;

import econo.project1.common.NotFoundException;
import econo.project1.kakao.KakaoService;
import econo.project1.kakao.KakaoUserInfoResponseDto;
import econo.project1.member.Member;
import econo.project1.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 카카오 로그인 -> JWT 발급 (별도 프론트용).
 * 흐름: 프론트가 카카오에서 받은 code 를 POST /api/auth/kakao 로 전달 -> 서버가 JWT 반환.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoService kakaoService;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    @Value("${kakao.client_id}")
    private String kakaoClientId;

    @Value("${kakao.redirect_uri}")
    private String kakaoRedirectUri;

    // (선택) 프론트가 사용할 카카오 인가 URL 을 만들어 반환
    @GetMapping("/kakao/login-url")
    public Map<String, String> kakaoLoginUrl() {
        String url = "https://kauth.kakao.com/oauth/authorize"
                + "?response_type=code"
                + "&client_id=" + kakaoClientId
                + "&redirect_uri=" + URLEncoder.encode(kakaoRedirectUri, StandardCharsets.UTF_8);
        return Map.of("url", url);
    }

    // 인가코드 -> 토큰 교환 -> 회원 저장/조회 -> JWT 발급
    @PostMapping("/kakao")
    public LoginResponse kakaoLogin(@RequestBody KakaoLoginRequest request) {
        String kakaoAccessToken = kakaoService.getAccessTokenFromKakao(request.code());
        KakaoUserInfoResponseDto userInfo = kakaoService.getUserInfo(kakaoAccessToken);

        Member member = kakaoService.getOrSaveMember(
                userInfo.getId(),
                userInfo.getKakaoAccount().getProfile().getNickName(),
                kakaoAccessToken);

        String token = jwtProvider.createToken(member.getId());
        return new LoginResponse(token, MemberResponse.from(member));
    }

    // 현재 로그인 회원 정보
    @GetMapping("/me")
    public MemberResponse me(@LoginMember Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다. id=" + memberId));
        return MemberResponse.from(member);
    }

    // 카카오 로그아웃(토큰 만료). JWT 자체는 클라이언트가 폐기.
    @PostMapping("/logout")
    public void logout(@LoginMember Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다. id=" + memberId));
        if (member.getAccessToken() != null) {
            kakaoService.logout(member.getAccessToken());
        }
    }
}
