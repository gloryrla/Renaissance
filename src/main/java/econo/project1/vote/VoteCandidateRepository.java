package econo.project1.vote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteCandidateRepository extends JpaRepository<VoteCandidate, Long> {

    List<VoteCandidate> findByVote(Vote vote);

    void deleteByVote(Vote vote);
}
