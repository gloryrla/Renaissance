package econo.project1.menu;

import econo.project1.common.Cuisine;
import econo.project1.common.Restriction;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;


/**
 * 외식 메뉴, 각 메뉴는 하나의 cuisine과 포함하는 제한 성분 집합을 가진다.
 */

@Getter
@Entity
public class Menu {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Cuisine cuisine;

    /**
     * 이 메뉴가 포함하는 제한 성분, 2단계 하드필터에서 사용
     * 메뉴 수가 적고 enum이라 @ElementCollectin으로 단순하게 저장
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "menu_restriction", joinColumns = @JoinColumn(name = "menu_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "restriction")
    private Set<Restriction> restrictions = EnumSet.noneOf(Restriction.class);



    protected Menu() {
    }

    public Menu(String name, Cuisine cuisine, Set<Restriction> restrictions) {
        this.name = name;
        this.cuisine = cuisine;
        this.restrictions = (restrictions == null || restrictions.isEmpty())
                ? EnumSet.noneOf(Restriction.class)
                : EnumSet.copyOf(restrictions);
    }



}
