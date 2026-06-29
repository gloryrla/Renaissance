package econo.project1.vote;

import econo.project1.common.NotFoundException;
import econo.project1.group.Group;
import econo.project1.group.GroupMemberRepository;
import econo.project1.group.GroupRepository;
import econo.project1.member.Member;
import econo.project1.member.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

@Tag(name = "Vote", description = "투표 생성, 실행, 삭제, 조회 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VoteController {

    private final VoteRepository voteRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MemberRepository memberRepository;

    // ① 그룹에 투표 생성
    @Operation(
            summary = "투표 생성",
            description = "그룹에 새로운 투표를 생성하는 엔드포인트. participantMemberIds 로 참여자를 지정하며(최소 1명), 방장은 자동 포함된다."
    )
    @ApiResponse(responseCode = "200", description = "생성된 투표 정보(Vote)")
    @PostMapping("/groups/{groupId}/votes")
    public Vote createVote(@PathVariable Long groupId,
                           @RequestBody VoteCreateRequest request) {
        if (request.school() == null) {
            throw new IllegalArgumentException("학교(school)는 필수입니다.");
        }
        if (request.participantMemberIds() == null || request.participantMemberIds().isEmpty()) {
            throw new IllegalArgumentException("참여자를 한 명 이상 선택하세요.");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("그룹이 존재하지 않습니다. id=" + groupId));

        // 참여자 = 요청 목록 + 방장(자동 포함)
        Set<Long> participants = new HashSet<>(request.participantMemberIds());
        participants.add(group.getOwner().getId());

        // 모든 참여자가 이 그룹의 멤버인지 검증
        for (Long memberId : participants) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다. id=" + memberId));
            if (!groupMemberRepository.existsByGroupAndMember(group, member)) {
                throw new IllegalArgumentException("그룹원이 아닌 참여자가 있습니다. id=" + memberId);
            }
        }

        Vote vote = Vote.builder()
                .title(request.title())
                .deadline(request.deadline())
                .group(group)
                .school(request.school())
                .participantMemberIds(participants)
                .build();
        return voteRepository.save(vote);
    }
}
