package econo.project1.kakao;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메뉴명으로 주변 식당 검색 (카카오 로컬 API). JSON 반환.
 */
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantApiController {

    private final KakaoLocalService kakaoLocalService;

    @GetMapping
    public KakaoLocalResponseDto search(@RequestParam String menu,
                                        @RequestParam(defaultValue = "1") Integer page) {
        return kakaoLocalService.getRestaurantList(menu, page);
    }
}
