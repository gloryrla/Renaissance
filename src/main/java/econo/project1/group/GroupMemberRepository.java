package econo.project1.group;

import econo.project1.group.Group;
import econo.project1.group.GroupMember;
import econo.project1.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByGroup(Group group);
    List<GroupMember> findByMember(Member member);

    boolean existsByGroupAndMember(Group group, Member member);

    Optional<GroupMember> findByGroupAndMember(Group group, Member member);

    void deleteByGroup(Group group);

    @Override
    Optional<GroupMember> findById(Long aLong);
}
