package econo.project1.vote;

import econo.project1.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BallotRepository extends JpaRepository<Ballot, Long> {

    List<Ballot> findByVote(Vote vote);

    void deleteByVoteAndMember(Vote vote, Member member);

    void deleteByVote(Vote vote);
}
