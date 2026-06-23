package econo.project1.group;


import econo.project1.group.Group;
import econo.project1.group.GroupMember;
import econo.project1.member.Member;
import econo.project1.group.GroupMemberRepository;
import econo.project1.group.GroupRepository;
import econo.project1.member.MemberRepository;
import econo.project1.group.GroupRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public GroupMember createGroup(String name, String description, Member member) {
        Group group = Group.builder()
                .name(name)
                .description(description)
                .owner(member)
                .inviteCode(generateValue())
                .build();
        groupRepository.save(group);

        GroupMember groupMember = GroupMember.builder()
                .group(group)
                .member(member)
                .role(GroupRole.OWNER)
                .build();
        groupMemberRepository.save(groupMember);

        return groupMember;
    }

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public Group findById(Long id) {
        return groupRepository.findById(id).orElse(null);
    }


    private String generateValue() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }



}
