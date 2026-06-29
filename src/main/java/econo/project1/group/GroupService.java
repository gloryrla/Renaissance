package econo.project1.group;


import econo.project1.common.ForbiddenException;
import econo.project1.common.NotFoundException;
import econo.project1.group.Group;
import econo.project1.group.GroupMember;
import econo.project1.member.Member;
import econo.project1.group.GroupMemberRepository;
import econo.project1.group.GroupRepository;
import econo.project1.member.MemberRepository;
import econo.project1.group.GroupRole;
import econo.project1.vote.BallotRepository;
import econo.project1.vote.Vote;
import econo.project1.vote.VoteCandidateRepository;
import econo.project1.vote.VotePreferenceRepository;
import econo.project1.vote.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MemberRepository memberRepository;
    private final VoteRepository voteRepository;
    private final VotePreferenceRepository preferenceRepository;
    private final VoteCandidateRepository candidateRepository;
    private final BallotRepository ballotRepository;

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

    // 그룹 삭제 (방장 전용). 그룹의 모든 투표 + 연관 데이터 + 그룹원까지 함께 삭제한다.
    @Transactional
    public void deleteGroup(Long groupId, Long actingMemberId) {
        Group group = getGroup(groupId);
        requireOwner(group, actingMemberId);

        for (Vote vote : voteRepository.findByGroup(group)) {
            preferenceRepository.deleteByVote(vote);
            candidateRepository.deleteByVote(vote);
            ballotRepository.deleteByVote(vote);
            voteRepository.delete(vote);
        }
        groupMemberRepository.deleteByGroup(group);
        groupRepository.delete(group);
    }

    // 그룹원 추방 (방장 전용). 추방된 사람이 그룹 투표들에 낸 선호/호불호를 지우고
    // 각 투표의 참여자 목록에서도 빼서, 그 사람이 빠져도 투표가 영향받지 않게 한다.
    @Transactional
    public void removeMember(Long groupId, Long actingMemberId, Long targetMemberId) {
        Group group = getGroup(groupId);
        requireOwner(group, actingMemberId);

        if (group.getOwner() != null && group.getOwner().getId().equals(targetMemberId)) {
            throw new IllegalArgumentException("방장은 그룹에서 제거할 수 없습니다.");
        }

        Member target = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다. id=" + targetMemberId));
        GroupMember groupMember = groupMemberRepository.findByGroupAndMember(group, target)
                .orElseThrow(() -> new NotFoundException("그룹원이 아닙니다. memberId=" + targetMemberId));

        for (Vote vote : voteRepository.findByGroup(group)) {
            preferenceRepository.deleteByVoteAndMember(vote, target);
            ballotRepository.deleteByVoteAndMember(vote, target);
            if (vote.getParticipantMemberIds().remove(targetMemberId)) {
                voteRepository.save(vote);
            }
        }
        groupMemberRepository.delete(groupMember);
    }

    private Group getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("그룹이 존재하지 않습니다. id=" + groupId));
    }

    private void requireOwner(Group group, Long actingMemberId) {
        if (group.getOwner() == null || !group.getOwner().getId().equals(actingMemberId)) {
            throw new ForbiddenException("방장만 할 수 있는 동작입니다.");
        }
    }


    private String generateValue() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }



}
