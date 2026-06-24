package econo.project1.menu;

import econo.project1.common.Cuisine;
import econo.project1.common.Restriction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 앱 시작 시 menu-data.csv 를 읽어 메뉴를 1회 적재한다.
 * - 멱등: 이미 메뉴가 있으면 건너뜀 (재시작/재배포 시 중복 방지)
 * - CSV 형식: name,cuisine,allergens   (allergens 는 ';' 로 구분)
 *   cuisine / allergens 값은 Cuisine / Restriction enum 이름과 1:1 일치.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.menu-seed.enabled", havingValue = "true", matchIfMissing = true)
public class MenuDataInitializer implements CommandLineRunner {

    private static final String CSV_PATH = "menu-data.csv";

    private final MenuRepository menuRepository;

    @Override
    public void run(String... args) throws Exception {
        if (menuRepository.count() > 0) {
            log.info("[MenuDataInitializer] 메뉴가 이미 존재하여 적재를 건너뜁니다. (count={})", menuRepository.count());
            return;
        }

        List<Menu> menus = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource(CSV_PATH).getInputStream(), StandardCharsets.UTF_8))) {

            String line = br.readLine(); // 헤더 스킵
            int lineNo = 1;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;
                String[] cols = line.split(",", -1);
                if (cols.length < 2) {
                    log.warn("[MenuDataInitializer] {}행 형식 오류, 건너뜀: {}", lineNo, line);
                    continue;
                }
                String name = cols[0].trim();
                Cuisine cuisine = Cuisine.valueOf(cols[1].trim());
                Set<Restriction> allergens = parseAllergens(cols.length > 2 ? cols[2] : "");
                menus.add(new Menu(name, cuisine, allergens));
            }
        }

        menuRepository.saveAll(menus);
        log.info("[MenuDataInitializer] 메뉴 {}개 적재 완료.", menus.size());
    }

    private Set<Restriction> parseAllergens(String raw) {
        Set<Restriction> set = EnumSet.noneOf(Restriction.class);
        if (raw == null || raw.isBlank()) return set;
        for (String code : raw.split(";")) {
            String c = code.trim();
            if (!c.isEmpty()) set.add(Restriction.valueOf(c));
        }
        return set;
    }
}
