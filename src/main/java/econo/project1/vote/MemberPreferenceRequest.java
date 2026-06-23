package econo.project1.vote;

import econo.project1.common.Cuisine;
import econo.project1.common.Restriction;

import java.util.Set;

/**
 * 그룹원 한 명의 추천 입력값.
 * dislikedCuisines: 오늘 안 당기는 카테고리, restrictions: 알레르기/식이 제한.
 */
public record MemberPreferenceRequest(
        Long memberId,
        Set<Cuisine> dislikedCuisines,
        Set<Restriction> restrictions
) {
}
