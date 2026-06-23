package econo.project1.vote;


import econo.project1.menu.Menu;
import econo.project1.common.Cuisine;
import econo.project1.common.Restriction;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 그룹 메뉴 추천 1 ~ 3단계
 * 핵심 원칙: 한명이라도 못/안 먹는 메뉴는 후보에 나오지 않는다.
 * 1) 알레르기 - 식이 제한 -> 하드필터 (절대 안화 안 함)
 * 2) 카테고리 비선호 -> 하드필터 (단, 후보 부족시에만 단계적 완화)
 * 3) 살아남은 풀에서 랜덤 3개
 */

@Service
public class RecommendationService {


    private static final int CANDIDATE_COUNT = 3;

    private final Random random;

    public RecommendationService() {
        this(new Random());
    }


    // 테스트에서 시드 고정용
    public RecommendationService(Random random) {
        this.random = random;
    }

    public CandidateResult recommend(List<Menu> allMenu, List<MemberDepreference> members) {
        //그룹 단위 제약 집계
        Set<Restriction> groupRestrictions = unionRestrictions(members);  //알레르기/식이제한 합집합
        Set<Cuisine> dislikeCuisines = unionDislikeCuisines(members); //비선호 카테고리 합집합

        //1단계 알레르기 식이 제한 하드필터
        //전체 메뉴데이터에서
        List<Menu> safeMenus = allMenu.stream()
                .filter(menu -> Collections.disjoint(menu.getRestrictions(), groupRestrictions))  //stream 메뉴 하나식 알레르기 disjoin true(서로소)면 통과
                .collect(Collectors.toList());

        //알레르기만으로도 후보를 못 만들면 추천 불가
        if (safeMenus.size() < CANDIDATE_COUNT) {
            return new CandidateResult(pickRandom(safeMenus, CANDIDATE_COUNT), false, Collections.emptySet(), true);
        }


        //2단계 카테고리 비선호 하드필터
        List<Menu> pool = safeMenus.stream()
                .filter(menu -> !dislikeCuisines.contains(menu.getCuisine()))
                .collect(Collectors.toList());

        //3단계 진입 전 풀이 충분하면 바로 랜덤 3개
        if (pool.size() >= CANDIDATE_COUNT) {
            return new CandidateResult(pickRandom(pool, CANDIDATE_COUNT), false, Collections.emptySet(), false);
        }

        //완화 로직: 비 선호표가 적은 cuisine부터 단계적으로 되살린다.
        return relaxAndFill(safeMenus, members, dislikeCuisines);



    }


    private CandidateResult relaxAndFill(List<Menu> safeMenu,
                                         List<MemberDepreference> Members,
                                         Set<Cuisine> dislikedCuisines) {
        Map<Cuisine, Long> dislikeVotes = new EnumMap<>(Cuisine.class);
        for (MemberDepreference m : Members) {
            for (Cuisine c : m.getDislikedCuisines()) {
                dislikeVotes.merge(c, 1L, Long::sum);
            }
        }

        List<Cuisine> relaxOrder = dislikedCuisines.stream()
                .sorted(Comparator.comparingLong(c -> dislikeVotes.getOrDefault(c, 0L)))
                .collect(Collectors.toList());

        Set<Cuisine> allowed = EnumSet.allOf(Cuisine.class);
        allowed.removeAll(dislikedCuisines);
        Set<Cuisine> relaxed = EnumSet.noneOf(Cuisine.class);

        List<Menu> pool = filterByCuisines(safeMenu, allowed);

        for (Cuisine c : relaxOrder) {
            if (pool.size() >= CANDIDATE_COUNT) break;
            allowed.add(c);
            relaxed.add(c);
            pool = filterByCuisines(safeMenu, allowed);
        }

        boolean stillShort = pool.size() < CANDIDATE_COUNT;
        return new CandidateResult(
                pickRandom(pool, CANDIDATE_COUNT),
                !relaxed.isEmpty(),
                relaxed,
                stillShort
        );
    }


    private List<Menu> filterByCuisines(List<Menu> menus, Set<Cuisine> allowed) {
        return menus.stream()
                .filter(m -> allowed.contains(m.getCuisine()))
                .collect(Collectors.toList());
    }



    // collection.forEach(변수 -> 반복처리(변수))
    private Set<Restriction> unionRestrictions(List<MemberDepreference> members) {
        Set<Restriction> set = EnumSet.noneOf(Restriction.class);
        members.forEach(member -> set.addAll(member.getRestrictions()));
        return set;
    }

    private Set<Cuisine> unionDislikeCuisines(List<MemberDepreference> members) {
        Set<Cuisine> set = EnumSet.noneOf(Cuisine.class);
        members.forEach(member -> set.addAll(member.getDislikedCuisines()));
        return set;
    }


    // 풀에서 중복 없이 랜덤 n개, n개보다 적으면 있는 만큼만
    private List<Menu> pickRandom(List<Menu> pool, int n) {
        if (pool.size() <= n) {
            List<Menu> all = new ArrayList<>(pool);
            Collections.shuffle(all, random);
            return all;
        }

        List<Menu> copy = new ArrayList<>(pool);
        Collections.shuffle(copy, random);
        return new ArrayList<>(copy.subList(0, n));
    }
}
