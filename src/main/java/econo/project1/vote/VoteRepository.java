package econo.project1.vote;

import econo.project1.group.Group;
import econo.project1.vote.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    List<Vote> findByGroup(Group group);
}
