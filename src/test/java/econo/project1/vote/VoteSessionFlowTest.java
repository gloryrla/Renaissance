package econo.project1.vote;

import econo.project1.common.ForbiddenException;
import econo.project1.common.Cuisine;
import econo.project1.common.Restriction;
import econo.project1.group.Group;
import econo.project1.group.GroupMember;
import econo.project1.group.GroupMemberRepository;
import econo.project1.group.GroupRepository;
import econo.project1.group.GroupRole;
import econo.project1.member.Member;
import econo.project1.member.MemberRepository;
import econo.project1.menu.Menu;
import econo.project1.menu.MenuRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ②~⑤ 전체 흐름 + 권한 검증 동작 테스트. 인메모리 H2로 실제 스프링 컨텍스트를 띄워서 검증한다.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:votetest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.session.store-type=none",   // 테스트에선 JDBC 세션 저장 비활성화
        "app.menu-seed.enabled=false",      // CSV 자동 적재 끄고 테스트용 메뉴만 사용
        // 시크릿 환경변수 대체(컨텍스트 로딩용 더미)
        "kakao.client_id=test", "kakao.secret_id=test"
})
class VoteSessionFlowTest {

    @Autowired VoteSessionService voteSessionService;
    @Autowired MemberRepository memberRepository;
    @Autowired MenuRepository menuRepository;
    @Autowired GroupRepository groupRepository;
    @Autowired GroupMemberRepository groupMemberRepository;
    @Autowired VoteRepository voteRepository;

    @Test
    void 전체_투표_흐름() {
        // ===== 시드 데이터 =====
        Member a = saveMember("호철", 1001L);   // CHINESE 비선호 + NUT 알레르기
        Member b = saveMember("영희", 1002L);   // MILK 알레르기
        Member d = saveMember("동길", 1003L);   // KOREAN 비선호 + EGG 알레르기

        Menu m1 = menuRepository.save(menu("김밥", Cuisine.KOREAN));
        Menu m2 = menuRepository.save(menu("비빔밥", Cuisine.KOREAN, Restriction.EGG));
        Menu m3 = menuRepository.save(menu("짜장면", Cuisine.CHINESE));        // CHINESE -> 2단계 제외
        Menu m4 = menuRepository.save(menu("스테이크", Cuisine.WESTERN, Restriction.SOY));
        Menu m5 = menuRepository.save(menu("초밥", Cuisine.JAPANESE));
        Menu m6 = menuRepository.save(menu("팟타이", Cuisine.ASIAN, Restriction.NUT));    // NUT -> 1단계 제외
        Menu m7 = menuRepository.save(menu("피자", Cuisine.WESTERN, Restriction.MILK));   // MILK -> 1단계 제외
        Menu m8 = menuRepository.save(menu("탕수육", Cuisine.CHINESE, Restriction.SEAFOOD)); // CHINESE -> 2단계 제외

        Group group = groupRepository.save(Group.builder().name("점심팟").build());
        joinGroup(group, a, b, d);   // 세 명 모두 그룹원
        Vote vote = voteRepository.save(Vote.builder().title("오늘 점심").group(group).build());
        Long voteId = vote.getId();

        // 생성 직후 상태 확인
        assertEquals(VoteStatus.RECOMMENDING, voteSessionService.getDetail(voteId, a.getId()).status());

        // ===== ② 선호 제출 =====
        voteSessionService.submitPreference(voteId, a.getId(),
                new MemberPreferenceRequest(a.getId(), EnumSet.of(Cuisine.CHINESE), EnumSet.of(Restriction.NUT)));
        voteSessionService.submitPreference(voteId, b.getId(),
                new MemberPreferenceRequest(b.getId(), Collections.emptySet(), EnumSet.of(Restriction.MILK)));
        voteSessionService.submitPreference(voteId, d.getId(),
                new MemberPreferenceRequest(d.getId(), EnumSet.of(Cuisine.KOREAN), EnumSet.of(Restriction.EGG)));

        // 모두 제출했으므로 대기자는 없어야 한다
        assertTrue(voteSessionService.pendingMembers(voteId, a.getId()).isEmpty());

        // ===== ③ 추천 =====
        RecommendResultResponse rec = voteSessionService.recommend(voteId, a.getId());

        assertEquals(3, rec.candidates().size(), "후보는 3개여야 한다");
        assertFalse(rec.impossible());

        Set<Long> candidateIds = new HashSet<>();
        for (CandidateMenuResponse c : rec.candidates()) {
            candidateIds.add(c.menuId());
            // 알레르기 메뉴(NUT/MILK/EGG)는 절대 후보에 없어야 한다
            assertNotEquals(m6.getId(), c.menuId(), "NUT 메뉴는 제외돼야 한다");
            assertNotEquals(m7.getId(), c.menuId(), "MILK 메뉴는 제외돼야 한다");
            assertNotEquals(m2.getId(), c.menuId(), "EGG 메뉴는 제외돼야 한다");
        }
        assertEquals(VoteStatus.VOTING, voteSessionService.getDetail(voteId, a.getId()).status());

        // ===== ④ 호/불호 투표 =====
        // 두 멤버 모두 첫 후보를 LIKE, 나머지는 DISLIKE
        List<Long> ids = new ArrayList<>(candidateIds);
        Long favorite = ids.get(0);

        voteSessionService.submitBallot(voteId, a.getId(), new BallotRequest(a.getId(), ballot(ids, favorite)));
        voteSessionService.submitBallot(voteId, b.getId(), new BallotRequest(b.getId(), ballot(ids, favorite)));

        // 아직 동길이 투표 전이라 자동 집계되지 않고 VOTING 유지
        assertEquals(VoteStatus.VOTING, voteSessionService.getDetail(voteId, a.getId()).status(),
                "전원이 투표하기 전에는 집계되지 않아야 한다");

        // ===== ⑤ 마지막 그룹원까지 투표하면 자동 집계 =====
        voteSessionService.submitBallot(voteId, d.getId(), new BallotRequest(d.getId(), ballot(ids, favorite)));

        VoteDetailResponse detail = voteSessionService.getDetail(voteId, a.getId());
        assertEquals(VoteStatus.CLOSED, detail.status(), "전원 투표 시 자동 집계되어 CLOSED 여야 한다");
        assertNotNull(detail.resultMenu());
        assertEquals(favorite, detail.resultMenu().menuId(), "호가 가장 많은 메뉴가 선정돼야 한다");

        System.out.println("최종 선정 메뉴 = " + detail.resultMenu().name() + " (id=" + detail.resultMenu().menuId() + ")");
    }

    @Test
    void 비그룹원은_참여할_수_없다() {
        Member owner = saveMember("주인", 2001L);
        Member outsider = saveMember("외부인", 2002L);
        Group group = groupRepository.save(Group.builder().name("폐쇄팟").build());
        joinGroup(group, owner);   // outsider 는 그룹원 아님
        Vote vote = voteRepository.save(Vote.builder().title("v").group(group).build());

        assertThrows(ForbiddenException.class, () ->
                voteSessionService.submitPreference(vote.getId(), outsider.getId(),
                        new MemberPreferenceRequest(outsider.getId(), Collections.emptySet(), Collections.emptySet())));
    }

    @Test
    void 추천_전에는_투표할_수_없다() {
        Member m = saveMember("회원", 3001L);
        Group group = groupRepository.save(Group.builder().name("팟").build());
        joinGroup(group, m);
        Vote vote = voteRepository.save(Vote.builder().title("v").group(group).build());

        // 상태가 RECOMMENDING 인데 ballot 시도 -> 단계 불일치
        assertThrows(IllegalStateException.class, () ->
                voteSessionService.submitBallot(vote.getId(), m.getId(),
                        new BallotRequest(m.getId(), Map.of(1L, VoteService.Choice.LIKE))));
    }

    @Test
    void 방장은_전원_투표_전에도_강제_마감할_수_있다() {
        Member owner = saveMember("방장", 4001L);
        Member m2 = saveMember("멤버", 4002L);
        Group group = groupRepository.save(Group.builder().name("강제마감팟").build());
        groupMemberRepository.save(GroupMember.builder().group(group).member(owner).role(GroupRole.OWNER).build());
        groupMemberRepository.save(GroupMember.builder().group(group).member(m2).role(GroupRole.MEMBER).build());

        menuRepository.save(menu("김밥", Cuisine.KOREAN));
        menuRepository.save(menu("초밥", Cuisine.JAPANESE));
        menuRepository.save(menu("스테이크", Cuisine.WESTERN));

        Vote vote = voteRepository.save(Vote.builder().title("점심").group(group).build());
        Long voteId = vote.getId();

        // 선호 제출 -> 추천
        voteSessionService.submitPreference(voteId, owner.getId(),
                new MemberPreferenceRequest(owner.getId(), Collections.emptySet(), Collections.emptySet()));
        RecommendResultResponse rec = voteSessionService.recommend(voteId, owner.getId());
        List<Long> ids = rec.candidates().stream().map(CandidateMenuResponse::menuId).toList();
        Long favorite = ids.get(0);

        // 방장만 투표하고 일반 멤버는 미투표 -> 자동 집계되지 않음
        voteSessionService.submitBallot(voteId, owner.getId(), new BallotRequest(owner.getId(), ballot(ids, favorite)));
        assertEquals(VoteStatus.VOTING, voteSessionService.getDetail(voteId, owner.getId()).status());

        // 일반 멤버는 강제 마감 불가
        assertThrows(ForbiddenException.class, () -> voteSessionService.forceClose(voteId, m2.getId()));

        // 방장은 강제 마감 가능 -> 모인 표로 즉시 집계
        CandidateMenuResponse winner = voteSessionService.forceClose(voteId, owner.getId());
        assertEquals(favorite, winner.menuId());
        assertEquals(VoteStatus.CLOSED, voteSessionService.getDetail(voteId, owner.getId()).status());
    }

    // ===== 헬퍼 =====

    private Member saveMember(String name, Long kakaoId) {
        return memberRepository.save(Member.builder()
                .name(name)
                .kakaoId(kakaoId)
                .accessToken("token-" + kakaoId)
                .build());
    }

    private void joinGroup(Group group, Member... members) {
        for (Member m : members) {
            groupMemberRepository.save(GroupMember.builder()
                    .group(group)
                    .member(m)
                    .role(GroupRole.MEMBER)
                    .build());
        }
    }

    private Menu menu(String name, Cuisine cuisine, Restriction... restrictions) {
        // 빈 알레르기를 빈 Set으로 넘겨도 안전한지(EnumSet.copyOf 함정 수정) 함께 검증
        return new Menu(name, cuisine,
                restrictions.length == 0 ? Collections.emptySet() : EnumSet.copyOf(Arrays.asList(restrictions)));
    }

    private Map<Long, VoteService.Choice> ballot(List<Long> candidateIds, Long favorite) {
        Map<Long, VoteService.Choice> choices = new HashMap<>();
        for (Long id : candidateIds) {
            choices.put(id, id.equals(favorite) ? VoteService.Choice.LIKE : VoteService.Choice.DISLIKE);
        }
        return choices;
    }
}
