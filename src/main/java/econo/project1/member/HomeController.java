package econo.project1.member;


import econo.project1.member.Member;
import econo.project1.member.MemberRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@RequiredArgsConstructor
public class HomeController {

    private final MemberRepository memberRepository;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) return "redirect:/login";

        Member loginMember = memberRepository.findById(memberId).orElse(null);
        if (loginMember == null) return "redirect:/login";

        model.addAttribute("loginMember", loginMember);
        return "home";
    }
}
