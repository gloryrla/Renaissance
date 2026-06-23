package econo.project1.vote;

import econo.project1.menu.Menu;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * ③ 추천 결과로 확정된 후보 메뉴. 한 투표당 최대 3개.
 */
@Entity
@Getter
@Setter
public class VoteCandidate {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id")
    private Vote vote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;
}
