package econo.project1.member;


import econo.project1.common.Restriction;
import jakarta.persistence.*;
import lombok.*;

import java.util.EnumSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    private String name;

    @Column(unique = true)
    private Long kakaoId;

    @Column(unique = true)
    private String accessToken;


    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "member_restriction", joinColumns = @JoinColumn(name = "member_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "restriction")
    private Set<Restriction> restrictions = EnumSet.noneOf(Restriction.class);


}
