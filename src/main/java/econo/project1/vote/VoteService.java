package econo.project1.vote;


import econo.project1.menu.Menu;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.List;

/**
 * 4 ~ 5단계: 후보 3개에 대한 호/불호 투표 집계 -> 최종 1개 선정
 * <p>
 * 메뉴 선정 규칙:
 * 1순위) 호(like) 수가 가장 많은 메뉴
 * 2순위) 동점이면 불호(dislike)가 적은 메뉴
 * 3순위) 그래도 동점이면 (like - dislike)가 큰 쪽
 */
@Service
public class VoteService {

    public enum Choice {LIKE, DISLIKE}

    // 한 메뉴에 대한 집계 결과
    @Getter
    public static class Tally {
        private final Menu  menu;
        private int likes;
        private int dislikes;


        public Tally(Menu menu) {
            this.menu = menu;
        }

        public int getNet() {
            return likes - dislikes;
        }


    }


    /**
     * @param candidates 후모 메뉴(3걔)
     * @param votes memberId -> (menuId -> Like/DISLIKE)
     * @return 호 최다 메뉴
     */
    public Menu decide(List<Menu> candidates,
                       Map<Long, Map<Long, Choice>> votes) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("후보 메뉴가 없습니다");
        }


        List<Tally> tallies = tally(candidates, votes);

        return tallies.stream()
                .max(Comparator
                        .comparingInt(Tally::getLikes)       // 1) 호 최다
                        .thenComparingInt(t -> -t.getDislikes())  // 2) 불호 최다
                        .thenComparingInt(Tally::getNet))   //3)순 최대
                .map(Tally::getMenu)
                .orElseThrow();


    }

    // 후보멸 호/불호 집계
    public List<Tally> tally(List<Menu> candidates,
                             Map<Long, Map<Long, Choice>> votes) {
        Map<Long, Tally> byMenuId = new LinkedHashMap<>();
        for (Menu menu : candidates) {
            byMenuId.put(menu.getId(), new Tally(menu));
        }


        for (Map<Long, Choice> memberVotes : votes.values()) {
            for (Map.Entry<Long, Choice> e : memberVotes.entrySet()) {
                Tally t = byMenuId.get(e.getKey());
                if (t == null)  continue;
                if (e.getValue() == Choice.LIKE) t.likes++;
                else t.dislikes++;
            }
        }

        return new ArrayList<>(byMenuId.values());
    }


}
