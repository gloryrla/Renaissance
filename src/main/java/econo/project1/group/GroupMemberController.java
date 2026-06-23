package econo.project1.group;


import econo.project1.group.Group;
import econo.project1.group.GroupMember;
import econo.project1.member.Member;
import econo.project1.group.GroupMemberRepository;
import econo.project1.group.GroupRepository;
import econo.project1.member.MemberRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class GroupMemberController {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MemberRepository memberRepository;

    @GetMapping("/group/{id}/members")
    public String groupMembers(@PathVariable Long id, Model model, HttpSession session) {
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) return "redirect:/login";
        Member loginMember = memberRepository.findById(memberId).orElse(null);
        if (loginMember == null) return "redirect:/login";
        model.addAttribute("loginMember", loginMember);

        Optional<Group> findGroup = groupRepository.findById(id);
        if (findGroup.isEmpty()) {
            model.addAttribute("errorMessage", "그룹이 존재하지 않습니다.");
            return "error";
        }
        List<GroupMember> findGroupMember = groupMemberRepository.findByGroup(findGroup.get());
        model.addAttribute("findInGroup", findGroupMember);

        return "/group/members";
    }
}
