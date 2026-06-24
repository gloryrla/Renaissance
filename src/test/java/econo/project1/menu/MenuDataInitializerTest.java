package econo.project1.menu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * menu-data.csv -> DB 적재가 enum 매핑까지 성공하는지 검증.
 * (cuisine/allergen 코드가 enum 과 안 맞으면 CommandLineRunner 가 valueOf 에서 터져 컨텍스트 로딩 실패)
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:menuseedtest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.session.store-type=none",
        "app.menu-seed.enabled=true",      // 시더 켜고 실제 적재 검증
        "kakao.client_id=test", "kakao.secret_id=test"
})
class MenuDataInitializerTest {

    @Autowired MenuRepository menuRepository;

    @Test
    void csv_메뉴_166개가_적재된다() {
        assertEquals(166, menuRepository.count(), "CSV 전체 행이 적재돼야 한다");
        // 모든 메뉴가 카테고리를 가진다 (매핑 성공)
        assertTrue(menuRepository.findAll().stream().allMatch(m -> m.getCuisine() != null));
    }
}
