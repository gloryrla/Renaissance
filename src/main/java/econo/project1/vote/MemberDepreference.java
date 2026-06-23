package econo.project1.vote;

import econo.project1.common.Cuisine;
import econo.project1.common.Restriction;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * 그룹원 한 명의 입력
 * dislikedCuisines: 오늘 안 당기는 카테고리(복수 선택) -> 1단계 하드 필터
 * restrictions: 알레르기, 식이 제한 -> 2단게 하드 필터
 */

@Getter
public class MemberDepreference {

    private final Long memberId;
    private final Set<Cuisine> dislikedCuisines;
    private final Set<Restriction> restrictions;


    public MemberDepreference(Long memberId, Set<Cuisine> dislikedCuisines, Set<Restriction> restrictions) {
        this.memberId = memberId;
        this.dislikedCuisines = (dislikedCuisines == null || dislikedCuisines.isEmpty())
                ? EnumSet.noneOf(Cuisine.class)
                : EnumSet.copyOf(dislikedCuisines);
        this.restrictions = (restrictions == null || restrictions.isEmpty())
                ? EnumSet.noneOf(Restriction.class)
                : EnumSet.copyOf(restrictions);
    }
}

