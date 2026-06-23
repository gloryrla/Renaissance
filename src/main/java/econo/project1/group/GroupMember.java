package econo.project1.group;

import econo.project1.member.Member;


import econo.project1.group.GroupRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


/*
GroupMember 테이블
id | group_id | member_id | role
1  |    1     |     1     | OWNER   ← 그룹1에 멤버1
2  |    1     |     2     | MEMBER  ← 그룹1에 멤버2
3  |    1     |     3     | MEMBER  ← 그룹1에 멤버3
4  |    2     |     1     | OWNER   ← 그룹2에 멤버1
 */

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // N개의 groupMember에 하나의 group
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    // N개의 groupMember에 하나의 member
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    private GroupRole role;  //OWNER, MEMBER

    private LocalDateTime joinDate;
}
