package econo.project1.kakao;

import econo.project1.member.Member;
import econo.project1.kakao.KakaoService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class KakaoLogoutController {

    private final KakaoService kakaoService;

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        kakaoService.logout((String) session.getAttribute("accessToken"));
        session.invalidate();
        return "redirect:/";
    }
}
