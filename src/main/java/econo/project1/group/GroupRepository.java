package econo.project1.group;

import econo.project1.group.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    Group findByInviteCode(String inviteCode);


}
