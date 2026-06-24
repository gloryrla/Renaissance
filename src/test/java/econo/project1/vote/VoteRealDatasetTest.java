package econo.project1.vote;

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
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 실제 메뉴 데이터셋(menu-data.csv, 166개)으로 추천~집계 전체 흐름 검증.
 * 시더를 켜고 컨텍스트를 띄워 진짜 데이터로 돌린다.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:realdataset;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.session.store-type=none",
        "app.menu-seed.enabled=true",          // 실제 CSV 적재 ON
        "kakao.client_id=test", "kakao.secret_id=test"
})
class VoteRealDatasetTest {

    @Autowired VoteSessionService voteSessionService;
    @Autowired MemberRepository memberRepository;
    @Autowired MenuRepository menuRepository;
    @Autowired GroupRepository groupRepository;
    @Autowired GroupMemberRepository groupMemberRepository;
    @Autowired VoteRepository voteRepository;

    @Test
    @Transactional   // 후보 메뉴의 LAZY 알레르기 컬렉션을 읽기 위해
    void 실제_데이터셋으로_추천하고_집계한다() {
        // 데이터셋이 적재됐는지 먼저 확인
        assertEquals(166, menuRepository.count());

        // 그룹원 3명 + 선호
        Member p1 = saveMember("민수", 9001L);   // 중식 비선호 + 견과(NUT) 알레르기
        Member p2 = saveMember("지은", 9002L);   // 우유(MILK)+계란(EGG) 알레르기
        Member p3 = saveMember("태현", 9003L);   // 일식 비선호
        Group group = groupRepository.save(Group.builder().name("점심모임").build());
        joinGroup(group, p1, p2, p3);
        Vote vote = voteRepository.save(Vote.builder().title("오늘 뭐 먹지").group(group).build());
        Long voteId = vote.getId();

        Set<Restriction> groupAllergens = EnumSet.of(Restriction.NUT, Restriction.MILK, Restriction.EGG);
        Set<Cuisine> disliked = EnumSet.of(Cuisine.KOREAN, Cuisine.JAPANESE);

        voteSessionService.submitPreference(voteId, p1.getId(),
                new MemberPreferenceRequest(p1.getId(), EnumSet.of(Cuisine.KOREAN), EnumSet.of(Restriction.NUT)));
        voteSessionService.submitPreference(voteId, p2.getId(),
                new MemberPreferenceRequest(p2.getId(), Collections.emptySet(), EnumSet.of(Restriction.MILK, Restriction.EGG)));
        voteSessionService.submitPreference(voteId, p3.getId(),
                new MemberPreferenceRequest(p3.getId(), EnumSet.of(Cuisine.JAPANESE), Collections.emptySet()));

        // ===== 추천 =====
        RecommendResultResponse rec = voteSessionService.recommend(voteId, p1.getId());
        assertEquals(3, rec.candidates().size());

        System.out.println("=== 실제 데이터셋 추천 결과 ===");
        System.out.println("완화여부=" + rec.relaxed() + " 완화카테고리=" + rec.relaxedCuisines());
        for (CandidateMenuResponse c : rec.candidates()) {
            Menu m = menuRepository.findById(c.menuId()).orElseThrow();
            System.out.println(" - " + c.name() + " [" + c.cuisine() + "] 알레르기=" + m.getRestrictions());

            // 1단계(알레르기)는 절대 완화 안 됨: 그룹 알레르기와 겹치면 안 된다
            assertTrue(Collections.disjoint(m.getRestrictions(), groupAllergens),
                    c.name() + " 에 그룹 알레르기 성분이 들어있음");

            // 완화가 없었다면 비선호 카테고리도 후보에 없어야 한다
            if (!rec.relaxed()) {
                assertFalse(disliked.contains(c.cuisine()),
                        c.name() + " 는 비선호 카테고리(" + c.cuisine() + ")라 후보에 없어야 함");
            }
        }

        // ===== 호/불호 -> 집계 =====
        List<Long> ids = rec.candidates().stream().map(CandidateMenuResponse::menuId).toList();
        Long favorite = ids.get(0);
        for (Member m : List.of(p1, p2, p3)) {
            Map<Long, VoteService.Choice> choices = new HashMap<>();
            for (Long id : ids) choices.put(id, id.equals(favorite) ? VoteService.Choice.LIKE : VoteService.Choice.DISLIKE);
            voteSessionService.submitBallot(voteId, m.getId(), new BallotRequest(m.getId(), choices));
        }

        CandidateMenuResponse winner = voteSessionService.decide(voteId, p1.getId());
        assertEquals(favorite, winner.menuId());
        System.out.println("최종 선정: " + winner.name() + " [" + winner.cuisine() + "]");
    }

    private Member saveMember(String name, Long kakaoId) {
        return memberRepository.save(Member.builder()
                .name(name).kakaoId(kakaoId).accessToken("token-" + kakaoId).build());
    }

    private void joinGroup(Group group, Member... members) {
        for (Member m : members) {
            groupMemberRepository.save(GroupMember.builder()
                    .group(group).member(m).role(GroupRole.MEMBER).build());
        }
    }
}
