package econo.project1.vote;

import econo.project1.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VotePreferenceRepository extends JpaRepository<VotePreference, Long> {

    List<VotePreference> findByVote(Vote vote);

    Optional<VotePreference> findByVoteAndMember(Vote vote, Member member);
}
