package rs.teslaris.core.service.impl.user;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.user.TutorialProgressRequestDTO;
import rs.teslaris.core.model.user.TutorialStatus;
import rs.teslaris.core.model.user.TutorialWalkthrough;
import rs.teslaris.core.repository.user.TutorialWalkthroughRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.user.TutorialWalkthroughService;

@Service
@RequiredArgsConstructor
@Traceable
public class TutorialWalkthroughServiceImpl extends JPAServiceImpl<TutorialWalkthrough>
    implements TutorialWalkthroughService {

    private final TutorialWalkthroughRepository tutorialWalkthroughRepository;

    private final UserRepository userRepository;


    @Override
    protected JpaRepository<TutorialWalkthrough, Integer> getEntityRepository() {
        return tutorialWalkthroughRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getCompletedTutorialKeys(Integer userId) {
        return tutorialWalkthroughRepository.findTutorialKeysByUserIdAndStatus(userId,
            TutorialStatus.FINISHED);
    }

    @Override
    @Transactional
    public void submitProgress(Integer userId, TutorialProgressRequestDTO request) {
        var walkthrough = tutorialWalkthroughRepository
            .findByUserIdAndTutorialKey(userId, request.getTutorialKey())
            .orElseGet(() -> {
                var newWalkthrough = new TutorialWalkthrough();
                newWalkthrough.setUser(userRepository.getReferenceById(userId));
                newWalkthrough.setTutorialKey(request.getTutorialKey());
                return newWalkthrough;
            });

        walkthrough.setStatus(request.getStatus());
        walkthrough.setStep(request.getStep());

        save(walkthrough);
    }

    @Override
    @Transactional
    public void deleteAllProgress(Integer userId) {
        tutorialWalkthroughRepository.deleteAllForUser(userId);
    }
}
