package rs.teslaris.dbinitialization;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.repository.commontypes.ResearchAreaRepository;
import rs.teslaris.core.util.seeding.SKOSLoader;

/**
 * Seeds research areas.
 * <p>
 * The order of the {@code saveAll} list below determines the generated IDs, which integration tests
 * rely on (they read area 1, update area 2 and delete area 3). The list is kept exactly as it was,
 * so entries may only ever be appended - never reordered or removed.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class ResearchAreaDataInitializer {

    private final ResearchAreaRepository researchAreaRepository;

    private final SKOSLoader skosLoader;


    /**
     * NOT COMPLETE - only the handful of areas the integration tests need.
     *
     * @return the deepest seeded area, used as a leaf reference by the testing data
     */
    public ResearchArea initializeResearchAreas(LanguageTag serbianTag) {
        var researchArea1 = new ResearchArea(new HashSet<>(Set.of(
            new MultiLingualContent(serbianTag, "Elektrotehnicko i racunarsko inzenjerstvo", 2))),
            new HashSet<>(), null, "elektrotehnicko i racunarsko inzenjerstvo", "TEST");
        var researchArea2 = new ResearchArea(new HashSet<>(Set.of(
            new MultiLingualContent(serbianTag, "Softversko inzenjerstvo", 2))),
            new HashSet<>(), researchArea1, "softversko inzenjerstvo", "TEST");
        var researchArea3 = new ResearchArea(new HashSet<>(Set.of(
            new MultiLingualContent(serbianTag, "Cybersecurity", 2))),
            new HashSet<>(), researchArea2, "cybersecurity", "TEST");

        researchAreaRepository.saveAll(List.of(researchArea1, researchArea2, researchArea3));

        return researchArea3;
    }

    /**
     * Loads the full research area taxonomy. Has to run after the seeding transaction commits.
     */
    public void loadResearchAreaTaxonomy() {
        skosLoader.loadResearchAreas();
    }
}
