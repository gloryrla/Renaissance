package econo.project1.menu;

import econo.project1.menu.Menu;
import econo.project1.menu.MenuRequest;
import econo.project1.menu.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuRepository menuRepository;

    // 전체 메뉴 조회
    @GetMapping
    public List<Menu> list() {
        return menuRepository.findAll();
    }

    // 메뉴 등록(시드)
    @PostMapping
    public Menu create(@RequestBody MenuRequest request) {
        Menu menu = new Menu(request.name(), request.cuisine(), request.restrictions());
        return menuRepository.save(menu);
    }
}
