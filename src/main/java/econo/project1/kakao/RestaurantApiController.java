package econo.project1.kakao;

import econo.project1.common.NotFoundException;
import econo.project1.common.School;
import econo.project1.vote.Vote;
import econo.project1.vote.VoteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메뉴명으로 주변 식당 검색 (카카오 로컬 API). JSON 반환.
 * voteId 를 주면 그 투표에 고정된 학교 좌표를 검색 중심으로 사용한다.
 */
@Tag(name = "Restaurant", description = "주변 식당 검색 API")
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantApiController {

    private final KakaoLocalService kakaoLocalService;
    private final VoteRepository voteRepository;

    @Operation(
            summary = "주변 식당 검색 (확정 메뉴 + 거리순)",
            description = "투표에 지정된 학교 좌표에서 가까운 순으로 식당을 검색하는 엔드포인트 (카카오 로컬 API). "
                    + "voteId 만 주면 그 투표의 확정 메뉴(resultMenu)로 자동 검색하고, menu 를 주면 그 키워드로 검색한다. "
                    + "voteId 가 없으면 기본 좌표를 사용한다."
    )
    @ApiResponse(responseCode = "200", description = "거리순 정렬된 식당 목록(KakaoLocalResponseDto)")
    @GetMapping
    public KakaoLocalResponseDto search(@RequestParam(required = false) String menu,
                                        @RequestParam(required = false) Long voteId,
                                        @RequestParam(defaultValue = "1") Integer page) {
        // voteId 없으면: 명시 메뉴 + 기본 좌표
        if (voteId == null) {
            return kakaoLocalService.getRestaurantList(requireMenu(menu, null), page);
        }

        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new NotFoundException("투표가 존재하지 않습니다. id=" + voteId));

        // 검색 키워드: menu 우선, 없으면 확정 메뉴(resultMenu)
        String keyword = requireMenu(menu, vote);

        School school = vote.getSchool();
        if (school == null) {
            return kakaoLocalService.getRestaurantList(keyword, page);
        }
        return kakaoLocalService.getRestaurantList(keyword, page,
                school.getX(), school.getY(), school.getRadius());
    }

    // 검색 키워드 해석: menu 가 있으면 그대로, 없으면 투표의 확정 메뉴명, 둘 다 없으면 400
    private String requireMenu(String menu, Vote vote) {
        if (menu != null && !menu.isBlank()) {
            return menu;
        }
        if (vote != null && vote.getResultMenu() != null) {
            return vote.getResultMenu().getName();
        }
        throw new IllegalArgumentException("검색할 메뉴가 없습니다. 투표를 마감하거나 menu 를 지정하세요.");
    }
}
