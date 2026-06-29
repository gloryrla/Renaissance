package econo.project1.vote;

import econo.project1.common.NotFoundException;
import econo.project1.group.Group;
import econo.project1.group.GroupRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Vote", description = "투표 생성, 실행, 삭제, 조회 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VoteController {

    private final VoteRepository voteRepository;
    private final GroupRepository groupRepository;

    // ① 그룹에 투표 생성
    @Operation(
            summary = "투표 생성",
            description = "그룹에 새로운 투표를 생성하는 엔드포인트"
    )
    @ApiResponse(responseCode = "200", description = "생성된 투표 정보(Vote)")
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
