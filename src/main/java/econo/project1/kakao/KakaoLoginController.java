package econo.project1.kakao;


import econo.project1.member.Member;
import econo.project1.kakao.KakaoUserInfoResponseDto;
import econo.project1.kakao.KakaoService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("")
public class KakaoLoginController {

    private final KakaoService kakaoService;

    @GetMapping("/callback")
    public String callback(@RequestParam("code") String code, HttpSession session) throws IOException {
        String accessToken = kakaoService.getAccessTokenFromKakao(code);
        KakaoUserInfoResponseDto userInfo = kakaoService.getUserInfo(accessToken);


//        KakaoUserInfoResponseDto.KakaoAccount.Profile profile = new KakaoUserInfoResponseDto.KakaoAccount.Profile();


        Member member = kakaoService.getOrSaveMember(userInfo.getId(), userInfo.kakaoAccount.profile.getNickName(), accessToken);
        session.setAttribute("memberId", member.getId());
        session.setAttribute("accessToken", accessToken);
        log.info("세션 저장 완료 ---> memberId: {}", member.getId());


        return "redirect:/";
    }
}
