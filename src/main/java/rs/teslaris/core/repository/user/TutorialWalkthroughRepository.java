package rs.teslaris.core.repository.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.user.TutorialStatus;
import rs.teslaris.core.model.user.TutorialWalkthrough;

@Repository
public interface TutorialWalkthroughRepository extends JpaRepository<TutorialWalkthrough, Integer> {

    @Query("SELECT tw FROM TutorialWalkthrough tw WHERE tw.user.id = :userId AND tw.tutorialKey = :tutorialKey")
    Optional<TutorialWalkthrough> findByUserIdAndTutorialKey(Integer userId, String tutorialKey);

    @Query("SELECT tw.tutorialKey FROM TutorialWalkthrough tw WHERE tw.user.id = :userId AND tw.status = :status")
    List<String> findTutorialKeysByUserIdAndStatus(Integer userId, TutorialStatus status);

    @Modifying
    @Query("DELETE FROM TutorialWalkthrough tw WHERE tw.user.id = :userId")
    void deleteAllForUser(Integer userId);
}
