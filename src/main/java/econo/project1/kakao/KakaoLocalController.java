package econo.project1.kakao;

import econo.project1.kakao.KakaoLocalResponseDto;
import econo.project1.kakao.KakaoLocalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Slf4j
@Controller
@RequiredArgsConstructor
public class KakaoLocalController {

    private final KakaoLocalService kakaoLocalService;

/*    @GetMapping("/restaurant/list")
    public String list(@RequestParam(required = false) String menu, Model model) {
        log.info("menu = {}",  menu);
        if (menu != null && !menu.isBlank()) {
            KakaoLocalResponseDto restaurantList = kakaoLocalService.getRestaurantList(menu, 1);
            model.addAttribute("restaurantList", restaurantList);
            model.addAttribute("menu", menu);
            model.addAttribute("page", 1);
            return "restaurant/list";

        }

        return "restaurant/search";
    }*/

     @GetMapping("/restaurant/list")
    public String listPaging( @RequestParam(name = "page", required = false, defaultValue = "1") Integer page,
                        @RequestParam(required = false) String menu, Model model) {
        if (menu != null && !menu.isBlank()) {
            KakaoLocalResponseDto restaurantList = kakaoLocalService.getRestaurantList(menu, page);
            model.addAttribute("restaurantList", restaurantList);
            model.addAttribute("menu", menu);
            model.addAttribute("page", page);
            return "restaurant/list";

        }

        return "restaurant/search";
    }




}
