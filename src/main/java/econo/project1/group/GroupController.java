package econo.project1.group;


import econo.project1.group.Group;
import econo.project1.member.Member;
import econo.project1.group.GroupRepository;
import econo.project1.member.MemberRepository;
import econo.project1.group.GroupMemberService;
import econo.project1.group.GroupService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final GroupMemberService groupMemberService;
    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;

    private Member getLoginMember(HttpSession session) {
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) return null;
        return memberRepository.findById(memberId).orElse(null);
    }


    //그룹 생성 get
    @GetMapping("/group/new")
    public String newGroupForm(HttpSession session, Model model) {
        Member loginMember = getLoginMember(session);
        if (loginMember == null) return "redirect:/login";
        model.addAttribute("loginMember", loginMember);
        return "group/new";  // templates/group/new.html
    }

    //그룹생성 post
    @PostMapping("/group/new")
    public String newGroup(@RequestParam String name,
                           @RequestParam String description, HttpSession session) {

        Member loginMember = getLoginMember(session);
        if (loginMember == null) return "redirect:/login";

        groupService.createGroup(name, description, loginMember);

        return "redirect:/group/list";
    }


    //그룹 리스트
    @GetMapping("/group/list")
    public String groupList(HttpSession session, Model model) {
        Member loginMember = getLoginMember(session);
        if (loginMember == null) return "redirect:/login";
        model.addAttribute("loginMember", loginMember);
        model.addAttribute("groups", groupService.getAllGroups());
        return "group/list";
    }

    @GetMapping("/group/in")
    public String inGroup(HttpSession session, Model model) {
        Member loginMember = getLoginMember(session);
        if (loginMember == null) return "redirect:/login";
        model.addAttribute("loginMember", loginMember);

        return "group/in";
    }

    @PostMapping("/group/in")
    public String inGroup(@RequestParam String inviteCode, HttpSession session, Model model) {
        Member loginMember = getLoginMember(session);
        Group joinGroup = groupRepository.findByInviteCode(inviteCode);

        // 존재하지 않은 그룹 inviteCode 입력시
        if (joinGroup == null) {
            model.addAttribute("errorMessage", "그룹이 존재하지 않습니다.");
            model.addAttribute("loginMember", loginMember);
            log.info("그룹 존재 ㄴㄴ");
            if (loginMember == null) return "redirect:/login";
            return "/group/in";

        }

        // 이미 들어간 그룹 inviteCode 입력시
        if (groupMemberService.duplicateGroupMember(joinGroup, loginMember)) {
            model.addAttribute("errorMessage", "이미 들어간 그룹입니다.");
            model.addAttribute("loginMember", loginMember);
            return "/group/in";
        }


        log.info("joinGroup = " + joinGroup);
        groupMemberService.createGroupMember(joinGroup, loginMember);


        return "redirect:/group/in";

    }

    @GetMapping("/group/newVote")
    public String newVote(HttpSession session, Model model) {
        Member loginMember = getLoginMember(session);
        if (loginMember == null) return "redirect:/login";
        model.addAttribute("loginMember", loginMember);
        return "group/newVote";  // templates/group/newVote.html
    }
}
