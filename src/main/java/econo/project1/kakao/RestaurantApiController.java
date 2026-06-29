package econo.project1.kakao;

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
 */
@Tag(name = "Restaurant", description = "주변 식당 검색 API")
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantApiController {

    private final KakaoLocalService kakaoLocalService;

    @Operation(
            summary = "주변 식당 검색",
            description = "메뉴명으로 주변 식당을 검색하는 엔드포인트 (카카오 로컬 API)"
    )
    @ApiResponse(responseCode = "200", description = "검색된 식당 목록(KakaoLocalResponseDto)")
    @GetMapping
    public KakaoLocalResponseDto search(@RequestParam String menu,
                                        @RequestParam(defaultValue = "1") Integer page) {
        return kakaoLocalService.getRestaurantList(menu, page);
    }
}
