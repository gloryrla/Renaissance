package econo.project1.vote;


import econo.project1.menu.Menu;
import econo.project1.common.Cuisine;
import econo.project1.common.Restriction;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 1 ~ 3단게 결과, 후보 메뉴와, 완화가 일어났는지, 어떤 cuisine을 되살렸는지 정보를 담는다.
 */
@Getter
public class CandidateResult {

    private final List<Menu> candidates;  //최종 후보(최대 3개, 부족하면 그 이하)

    private final boolean relaxed;                              //카테고리 완화가 발생했는가
    private final Set<Cuisine> relaxedCuisines;                //완화로 되살린 cuisine
    private final boolean impossible;                           //알레르기만으로 후보를 못 만든 경우


    public CandidateResult(List<Menu> candidates, boolean relaxed, Set<Cuisine> relaxedCuisines, boolean impossible) {
        this.candidates = candidates;
        this.relaxed = relaxed;
        this.relaxedCuisines = relaxedCuisines;
        this.impossible = impossible;
    }
}
