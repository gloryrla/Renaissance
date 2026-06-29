package econo.project1.vote;

import econo.project1.common.ForbiddenException;
import econo.project1.common.NotFoundException;
import econo.project1.group.Group;
import econo.project1.group.GroupMember;
import econo.project1.group.GroupMemberRepository;
import econo.project1.group.GroupRole;
import econo.project1.member.Member;
import econo.project1.member.MemberRepository;
import econo.project1.menu.Menu;
import econo.project1.menu.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 상태를 가진(stateful) 투표 진행 오케스트레이터.
 * 실제 추천/집계 알고리즘은 RecommendationService(1~3단계)와 VoteService(4~5단계)에 위임하고,
 * 여기서는 DB 저장, 단계 전환(VoteStatus), 권한/마감 검증을 책임진다.
 *
 *  ② submitPreference  -> 그룹원 선호 저장
 *  ③ recommend         -> 모인 선호로 후보 3개 계산 후 저장, 상태 VOTING
 *  ④ submitBallot      -> 후보에 대한 호/불호 저장
 *  ⑤ decide            -> 집계해 최종 메뉴 확정, 상태 CLOSED
 *
 * 모든 동작은 "투표가 속한 그룹의 멤버"만 가능하다. (actingMemberId = 로그인한 회원)
 */
@Service
@RequiredArgsConstructor
public class VoteSessionService {

    private final VoteRepository voteRepository;
    private final MemberRepository memberRepository;
    private final MenuRepository menuRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final VotePreferenceRepository preferenceRepository;
    private final VoteCandidateRepository candidateRepository;
    private final BallotRepository ballotRepository;
    private final RecommendationService recommendationService;
    private final VoteService voteService;

    // ② 선호 제출 (이미 냈으면 덮어쓰기)
    @Transactional
    public void submitPreference(Long voteId, Long actingMemberId, MemberPreferenceRequest request) {
        Vote vote = getVote(voteId);
        Member member = requireParticipant(vote, actingMemberId);
        requireStatus(vote, VoteStatus.RECOMMENDING);
        requireBeforeDeadline(vote);

        VotePreference pref = preferenceRepository.findByVoteAndMember(vote, member)
                .orElseGet(() -> {
                    VotePreference p = new VotePreference();
                    p.setVote(vote);
                    p.setMember(member);
                    return p;
                });
        pref.setDislikedCuisines(copyOrEmpty(request.dislikedCuisines()));
        pref.setRestrictions(copyOrEmpty(request.restrictions()));
        preferenceRepository.save(pref);
    }

    // ③ 추천 실행 -> 후보 저장 -> 상태 VOTING
    @Transactional
    public RecommendResultResponse recommend(Long voteId, Long actingMemberId) {
        Vote vote = getVote(voteId);
        requireGroupMember(vote, actingMemberId);
        requireStatus(vote, VoteStatus.RECOMMENDING);
        requireBeforeDeadline(vote);

        List<VotePreference> prefs = preferenceRepository.findByVote(vote);
        if (prefs.isEmpty()) {
            throw new IllegalStateException("제출된 선호가 없어 추천할 수 없습니다.");
        }

        List<MemberDepreference> members = prefs.stream()
                .map(p -> new MemberDepreference(
                        p.getMember().getId(),
                        p.getDislikedCuisines(),
                        p.getRestrictions()))
                .collect(Collectors.toList());

        CandidateResult result = recommendationService.recommend(menuRepository.findAll(), members);

        // 재추천 대비: 기존 후보 비우고 새로 저장
        candidateRepository.deleteByVote(vote);
        for (Menu menu : result.getCandidates()) {
            VoteCandidate candidate = new VoteCandidate();
            candidate.setVote(vote);
            candidate.setMenu(menu);
            candidateRepository.save(candidate);
        }

        vote.setRelaxed(result.isRelaxed());
        vote.setRelaxedCuisines(new HashSet<>(result.getRelaxedCuisines()));
        vote.setImpossible(result.isImpossible());
        vote.setStatus(VoteStatus.VOTING);

        return new RecommendResultResponse(
                result.getCandidates().stream().map(CandidateMenuResponse::from).collect(Collectors.toList()),
                result.isRelaxed(),
                result.getRelaxedCuisines(),
                result.isImpossible());
    }

    // ④ 호/불호 제출 (다시 내면 그 멤버 표를 덮어쓰기)
    @Transactional
    public void submitBallot(Long voteId, Long actingMemberId, BallotRequest request) {
        Vote vote = getVote(voteId);
        Member member = requireParticipant(vote, actingMemberId);
        requireStatus(vote, VoteStatus.VOTING);
        requireBeforeDeadline(vote);

        Set<Long> candidateMenuIds = candidateRepository.findByVote(vote).stream()
                .map(c -> c.getMenu().getId())
                .collect(Collectors.toSet());

        ballotRepository.deleteByVoteAndMember(vote, member);

        request.choices().forEach((menuId, choice) -> {
            if (!candidateMenuIds.contains(menuId)) {
                throw new IllegalArgumentException("후보에 없는 메뉴입니다. menuId=" + menuId);
            }
            Menu menu = menuRepository.findById(menuId)
                    .orElseThrow(() -> new NotFoundException("메뉴가 존재하지 않습니다. id=" + menuId));
            Ballot ballot = new Ballot();
            ballot.setVote(vote);
            ballot.setMember(member);
            ballot.setMenu(menu);
            ballot.setChoice(choice);
            ballotRepository.save(ballot);
        });

        // ⑤ 그룹원 전원이 호/불호를 제출하면 자동으로 집계해 마감한다(별도 마무리 버튼 없음)
        if (allMembersVoted(vote)) {
            aggregate(vote);
        }
    }

    // ⑤ 방장(OWNER) 강제 마감: 전원이 투표를 마치지 않았어도 그때까지 모인 표로 집계해 마감한다.
    @Transactional
    public CandidateMenuResponse forceClose(Long voteId, Long actingMemberId) {
        Vote vote = getVote(voteId);
        Member member = requireGroupMember(vote, actingMemberId);
        requireOwner(vote, member);
        requireStatus(vote, VoteStatus.VOTING);
        if (ballotRepository.findByVote(vote).isEmpty()) {
            throw new IllegalStateException("아무도 투표하지 않았습니다.");
        }
        return aggregate(vote);
    }

    // ⑤ 집계 -> 최종 메뉴 확정 -> 상태 CLOSED.
    // 그룹원 전원이 투표를 마쳤을 때 submitBallot 안에서 자동 호출되거나, 방장이 forceClose 로 호출한다.
    private CandidateMenuResponse aggregate(Vote vote) {
        List<Menu> candidates = candidateRepository.findByVote(vote).stream()
                .map(VoteCandidate::getMenu)
                .collect(Collectors.toList());

        // memberId -> (menuId -> LIKE/DISLIKE)
        Map<Long, Map<Long, VoteService.Choice>> votes = new HashMap<>();
        for (Ballot ballot : ballotRepository.findByVote(vote)) {
            votes.computeIfAbsent(ballot.getMember().getId(), k -> new HashMap<>())
                    .put(ballot.getMenu().getId(), ballot.getChoice());
        }

        Menu winner = voteService.decide(candidates, votes);
        vote.setResultMenu(winner);
        vote.setStatus(VoteStatus.CLOSED);
        return CandidateMenuResponse.from(winner);
    }

    // 참여자 전원이 적어도 한 표라도 제출했는지 검사
    private boolean allMembersVoted(Vote vote) {
        Set<Long> expected = participantIds(vote);
        if (expected.isEmpty()) {
            return false;
        }
        Set<Long> votedMemberIds = ballotRepository.findByVote(vote).stream()
                .map(b -> b.getMember().getId())
                .collect(Collectors.toSet());
        return votedMemberIds.containsAll(expected);
    }

    // 투표 삭제 (방장 OWNER 만). 연관 데이터(선호/후보/호불호)도 함께 삭제한다.
    @Transactional
    public void deleteVote(Long voteId, Long actingMemberId) {
        Vote vote = getVote(voteId);
        Member member = requireGroupMember(vote, actingMemberId);
        requireOwner(vote, member);

        preferenceRepository.deleteByVote(vote);
        candidateRepository.deleteByVote(vote);
        ballotRepository.deleteByVote(vote);
        voteRepository.delete(vote);
    }

    @Transactional(readOnly = true)
    public VoteDetailResponse getDetail(Long voteId, Long actingMemberId) {
        Vote vote = getVote(voteId);
        requireGroupMember(vote, actingMemberId);

        List<CandidateMenuResponse> candidates = candidateRepository.findByVote(vote).stream()
                .map(c -> CandidateMenuResponse.from(c.getMenu()))
                .collect(Collectors.toList());
        List<MemberSummaryResponse> participants = memberRepository.findAllById(participantIds(vote)).stream()
                .map(m -> new MemberSummaryResponse(m.getId(), m.getName()))
                .collect(Collectors.toList());
        Menu result = vote.getResultMenu();
        return new VoteDetailResponse(
                vote.getId(),
                vote.getTitle(),
                vote.getStatus(),
                vote.getSchool(),
                participants,
                candidates,
                result == null ? null : CandidateMenuResponse.from(result),
                vote.isRelaxed(),
                vote.getRelaxedCuisines(),
                vote.isImpossible());
    }

    // 아직 선호를 제출하지 않은 참여자 목록 (추천 전에 누구를 기다려야 하는지)
    @Transactional(readOnly = true)
    public List<MemberSummaryResponse> pendingMembers(Long voteId, Long actingMemberId) {
        Vote vote = getVote(voteId);
        requireGroupMember(vote, actingMemberId);

        Set<Long> submitted = preferenceRepository.findByVote(vote).stream()
                .map(p -> p.getMember().getId())
                .collect(Collectors.toSet());

        return memberRepository.findAllById(participantIds(vote)).stream()
                .filter(m -> !submitted.contains(m.getId()))
                .map(m -> new MemberSummaryResponse(m.getId(), m.getName()))
                .collect(Collectors.toList());
    }

    // ===== 헬퍼 =====

    private Vote getVote(Long voteId) {
        return voteRepository.findById(voteId)
                .orElseThrow(() -> new NotFoundException("투표가 존재하지 않습니다. id=" + voteId));
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("멤버가 존재하지 않습니다. id=" + memberId));
    }

    // 이 투표의 참여자 id 집합. 비어있으면(예전 데이터) 그룹 전원으로 폴백한다.
    private Set<Long> participantIds(Vote vote) {
        Set<Long> ids = vote.getParticipantMemberIds();
        if (ids == null || ids.isEmpty()) {
            return groupMemberRepository.findByGroup(vote.getGroup()).stream()
                    .map(gm -> gm.getMember().getId())
                    .collect(Collectors.toSet());
        }
        return ids;
    }

    // 제출 권한: 그룹원이면서 이 투표의 참여자여야 한다.
    private Member requireParticipant(Vote vote, Long memberId) {
        Member member = requireGroupMember(vote, memberId);
        if (!participantIds(vote).contains(memberId)) {
            throw new ForbiddenException("이 투표의 참여자만 제출할 수 있습니다.");
        }
        return member;
    }

    // 로그인 회원이 이 투표가 속한 그룹의 멤버인지 검증하고, 그 Member를 돌려준다.
    private Member requireGroupMember(Vote vote, Long memberId) {
        Group group = vote.getGroup();
        if (group == null) {
            throw new IllegalStateException("그룹에 속하지 않은 투표입니다. voteId=" + vote.getId());
        }
        Member member = getMember(memberId);
        if (!groupMemberRepository.existsByGroupAndMember(group, member)) {
            throw new ForbiddenException("이 그룹의 멤버만 참여할 수 있습니다.");
        }
        return member;
    }

    // 로그인 회원이 이 투표가 속한 그룹의 방장(OWNER)인지 검증한다.
    private void requireOwner(Vote vote, Member member) {
        GroupMember groupMember = groupMemberRepository.findByGroupAndMember(vote.getGroup(), member)
                .orElseThrow(() -> new ForbiddenException("이 그룹의 멤버만 참여할 수 있습니다."));
        if (groupMember.getRole() != GroupRole.OWNER) {
            throw new ForbiddenException("방장만 투표를 마감할 수 있습니다.");
        }
    }

    private void requireStatus(Vote vote, VoteStatus expected) {
        if (vote.getStatus() != expected) {
            throw new IllegalStateException(
                    "현재 단계(" + vote.getStatus() + ")에서 할 수 없는 동작입니다. 필요한 단계: " + expected);
        }
    }

    private void requireBeforeDeadline(Vote vote) {
        if (vote.getDeadline() != null && LocalDateTime.now().isAfter(vote.getDeadline())) {
            throw new IllegalStateException("마감된 투표입니다. (마감: " + vote.getDeadline() + ")");
        }
    }

    private static <T> Set<T> copyOrEmpty(Set<T> src) {
        return (src == null) ? new HashSet<>() : new HashSet<>(src);
    }
}
