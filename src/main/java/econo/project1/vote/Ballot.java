package econo.project1.vote;

import econo.project1.member.Member;
import econo.project1.menu.Menu;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * ④ 한 그룹원이 한 후보 메뉴에 던진 호/불호 한 표.
 * (vote, member, menu)당 한 행.
 */
@Entity
@Getter
@Setter
public class Ballot {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id")
    private Vote vote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;

    @Enumerated(EnumType.STRING)
    private VoteService.Choice choice;  // LIKE / DISLIKE
}
