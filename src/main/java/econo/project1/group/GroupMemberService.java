package econo.project1.group;


import econo.project1.group.Group;
import econo.project1.group.GroupMember;
import econo.project1.member.Member;
import econo.project1.group.GroupMemberRepository;
import econo.project1.group.GroupRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupMemberService {

    private final GroupMemberRepository groupMemberRepository;

    public GroupMember createGroupMember(Group group, Member member) {
        List<GroupMember> getGroupMember = groupMemberRepository.findByGroup(group);

        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(group);
        groupMember.setMember(member);
        groupMember.setRole(GroupRole.MEMBER);
        return groupMemberRepository.save(groupMember);
    }


    public boolean duplicateGroupMember(Group group, Member member) {
        List<GroupMember> getGroupMember = groupMemberRepository.findByGroup(group);
        for (GroupMember groupMember : getGroupMember) {
            if (groupMember.getMember().equals(member)) {
                return true;
            }
        }
        return false;
    }



}
