package econo.project1.vote;

import econo.project1.common.NotFoundException;
import econo.project1.group.Group;
import econo.project1.group.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VoteController {

    private final VoteRepository voteRepository;
    private final GroupRepository groupRepository;

    // ① 그룹에 투표 생성
    @PostMapping("/groups/{groupId}/votes")
    public Vote createVote(@PathVariable Long groupId,
                           @RequestBody VoteCreateRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("그룹이 존재하지 않습니다. id=" + groupId));

        Vote vote = Vote.builder()
                .title(request.title())
                .deadline(request.deadline())
                .group(group)
                .build();
        return voteRepository.save(vote);
    }
}
