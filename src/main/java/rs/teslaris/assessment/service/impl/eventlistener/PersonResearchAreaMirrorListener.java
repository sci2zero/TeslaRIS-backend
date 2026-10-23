package rs.teslaris.assessment.service.impl.eventlistener;

import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.repository.AssessmentResearchAreaRepository;
import rs.teslaris.core.applicationevent.PersonResearchAreasChangedEvent;

/**
 * Keeps the sub-areas of a person's assessment research area in step with the research areas held
 * on the person.
 * <p>
 * The person is the authority: whatever is set there wins, whether or not an assessment research
 * area code is present. The one thing this never does is create the assessment research area - a
 * person without one is simply not assessed, and inventing a row here would leave it with no code.
 * <p>
 * Deliberately synchronous: the copy is a single row and callers read it back straight after
 * saving, so eventual consistency would show stale sub-areas.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PersonResearchAreaMirrorListener {

    private final AssessmentResearchAreaRepository assessmentResearchAreaRepository;


    @EventListener
    @Transactional
    public void handle(PersonResearchAreasChangedEvent event) {
        var assessmentResearchArea =
            assessmentResearchAreaRepository.findForPersonId(event.personId());

        if (assessmentResearchArea.isEmpty()) {
            return;
        }

        var researchArea = assessmentResearchArea.get();
        researchArea.setResearchSubAreaIds(new HashSet<>(event.researchAreaIds()));
        assessmentResearchAreaRepository.save(researchArea);

        log.info("Mirrored {} research area(s) of person {} into its assessment sub-areas.",
            event.researchAreaIds().size(), event.personId());
    }
}
