package rs.teslaris.core.service.interfaces.user;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.user.TutorialProgressRequestDTO;
import rs.teslaris.core.model.user.TutorialWalkthrough;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface TutorialWalkthroughService extends JPAService<TutorialWalkthrough> {

    List<String> getCompletedTutorialKeys(Integer userId);

    void submitProgress(Integer userId, TutorialProgressRequestDTO request);

    void deleteAllProgress(Integer userId);
}
