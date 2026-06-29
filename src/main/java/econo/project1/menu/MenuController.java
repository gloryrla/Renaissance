package econo.project1.menu;

import econo.project1.menu.Menu;
import econo.project1.menu.MenuRequest;
import econo.project1.menu.MenuRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Menu", description = "메뉴 조회, 등록 API")
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuRepository menuRepository;

    // 전체 메뉴 조회
    @Operation(
            summary = "전체 메뉴 조회",
            description = "등록된 전체 메뉴 목록을 조회하는 엔드포인트"
    )
    @GetMapping
    public List<Menu> list() {
        return menuRepository.findAll();
    }

    // 메뉴 등록(시드)
    @Operation(
            summary = "메뉴 등록",
            description = "새로운 메뉴를 등록(시드)하는 엔드포인트"
    )
    @PostMapping
    public Menu create(@RequestBody MenuRequest request) {
        Menu menu = new Menu(request.name(), request.cuisine(), request.restrictions());
        return menuRepository.save(menu);
    }
}
