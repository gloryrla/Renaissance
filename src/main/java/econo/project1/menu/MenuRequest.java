package econo.project1.menu;

import econo.project1.common.Cuisine;
import econo.project1.common.Restriction;

import java.util.Set;

/**
 * 메뉴 등록(시드)용 요청.
 */
public record MenuRequest(
        String name,
        Cuisine cuisine,
        Set<Restriction> restrictions
) {
}
