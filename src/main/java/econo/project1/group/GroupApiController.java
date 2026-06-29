package econo.project1.group;

import econo.project1.auth.LoginMember;
import econo.project1.common.ForbiddenException;
import econo.project1.common.NotFoundException;
import econo.project1.member.Member;
import econo.project1.member.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 그룹 REST API. 모든 동작은 로그인 필요(@LoginMember).
 */
@Tag(name = "Group", description = "그룹 생성, 참여, 조회 API")
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupApiController {

    private final GroupService groupService;
    private final GroupMemberService groupMemberService;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MemberRepository memberRepository;

    // 그룹 생성 (생성자가 OWNER 로 자동 가입)
    @Operation(
            summary = "그룹 생성",
            description = "새 그룹을 생성하는 엔드포인트. 생성자가 OWNER 로 자동 가입된다."
    )
    @PostMapping
    public GroupResponse create(@LoginMember Long memberId, @RequestBody GroupCreateRequest request) {
        Member me = getMember(memberId);
        GroupMember owner = groupService.createGroup(request.name(), request.description(), me);
        return GroupResponse.from(owner.getGroup());
    }

    // 내가 속한 그룹 목록
    @Operation(
            summary = "내 그룹 목록 조회",
            description = "내가 속한 그룹 목록을 조회하는 엔드포인트"
    )
    @GetMapping
    public List<GroupResponse> myGroups(@LoginMember Long memberId) {
        Member me = getMember(memberId);
        return groupMemberRepository.findByMember(me).stream()
                .map(gm -> GroupResponse.from(gm.getGroup()))
                .toList();
    }

    // 초대코드로 그룹 참여
    @Operation(
            summary = "그룹 참여",
            description = "초대코드로 그룹에 참여하는 엔드포인트"
    )
    @PostMapping("/join")
    public GroupResponse join(@LoginMember Long memberId, @RequestBody JoinGroupRequest request) {
        Member me = getMember(memberId);
        Group group = groupRepository.findByInviteCode(request.inviteCode());
        if (group == null) {
            throw new NotFoundException("초대코드에 해당하는 그룹이 없습니다.");
        }
        if (groupMemberService.duplicateGroupMember(group, me)) {
            throw new IllegalStateException("이미 가입한 그룹입니다.");
        }
        groupMemberService.createGroupMember(group, me);
        return GroupResponse.from(group);
    }

    // 그룹원 목록 (해당 그룹 멤버만 조회 가능)
    @Operation(
            summary = "그룹원 목록 조회",
            description = "그룹원 목록을 조회하는 엔드포인트 (해당 그룹 멤버만 조회 가능)"
    )
    @GetMapping("/{groupId}/members")
    public List<GroupMemberResponse> members(@LoginMember Long memberId, @PathVariable Long groupId) {
        Member me = getMember(memberId);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("그룹이 존재하지 않습니다. id=" + groupId));
        if (!groupMemberRepository.existsByGroupAndMember(group, me)) {
            throw new ForbiddenException("이 그룹의 멤버만 조회할 수 있습니다.");
        }
        return groupMemberRepository.findByGroup(group).stream()
                .map(GroupMemberResponse::from)
                .toList();
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다. id=" + memberId));
    }
}
