package econo.project1.kakao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// 내 서버에서 카카오인증 서버로 로그인 창 넘기는 컨트롤러
@Controller
public class KakaoLoginViewController {

    @Value("${kakao.client_id}")
    private String KAKAO_CLIENT_ID;

    @Value(("${kakao.redirect_uri}"))
    private String KAKAO_REDIRECT_URI;

    @Value("${kakao.secret_id}")
    private String KAKAO_SECRET_ID;

    @GetMapping("/login")
    public String kakaoLogin(Model model) {

        String kakaoAuthUrl = "https://kauth.kakao.com/oauth/authorize";
        String responseType = "code";

        String kakaoLoginUrl = kakaoAuthUrl + "?" +
                "response_type=" + responseType +
                "&client_id=" + KAKAO_CLIENT_ID +
                "&redirect_uri=" + KAKAO_REDIRECT_URI;

        model.addAttribute("kakaoLoginUrl", kakaoLoginUrl);

        return "login";

    }
}
