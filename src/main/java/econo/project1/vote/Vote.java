package econo.project1.vote;

import econo.project1.common.Cuisine;
import econo.project1.group.Group;
import econo.project1.menu.Menu;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {

    @Id
    @GeneratedValue
    private Long id;

    private String title;  //투표 제목
    private LocalDateTime deadline;

    @ManyToOne(fetch = FetchType.LAZY)
    private Group group;

    // 진행 단계: RECOMMENDING(선호 수집) -> VOTING(호불호 투표) -> CLOSED(종료)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VoteStatus status = VoteStatus.RECOMMENDING;

    // 최종 선정 메뉴 (집계 전엔 null)
    @ManyToOne(fetch = FetchType.LAZY)
    private Menu resultMenu;

    // 추천(1~3단계) 부가정보
    private boolean relaxed;     // 카테고리 완화가 일어났는가
    private boolean impossible;  // 알레르기만으로 후보를 못 만든 경우

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "vote_relaxed_cuisine", joinColumns = @JoinColumn(name = "vote_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "cuisine")
    @Builder.Default
    private Set<Cuisine> relaxedCuisines = new HashSet<>();

}
