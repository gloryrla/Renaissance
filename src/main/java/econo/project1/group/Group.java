package econo.project1.group;

import econo.project1.member.Member;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "group_table")
@Builder
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @Column(unique = true)
    private String inviteCode;  // 초대 코드 (UUID 기반)

    private LocalDateTime createdAt;


    //n개 그룹 <-> 1개 맴버
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Member owner;  // 방장
}

