package rs.teslaris.revisioner.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.model.document.MonographType;
import rs.teslaris.core.repository.commontypes.CountryRepository;
import rs.teslaris.core.repository.commontypes.LanguageRepository;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.PublisherRepository;
import rs.teslaris.core.repository.institution.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.session.RestTemplateProvider;
import rs.teslaris.revisioner.model.EntityRevision;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.repository.DataQualityAssessmentRepository;
import rs.teslaris.revisioner.util.DataQualityCalculator;
import rs.teslaris.revisioner.util.ObjectMapperProvider;
import rs.teslaris.revisioner.util.RevisionHydratorRegistry;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentIndexer;

public class DataQualityCalculatorTest {

    private static final String PROFILE = "ptcris";

    private static final String VERSION = "1.0.0";

    private static final String CONTRIBUTORS_MISSING = "contributorsMissing";

    private static final String NO_MANAGED_CONTRIBUTOR = "noManagedContributor";

    private static final List<String> TARGETS = List.of("Document");

    private RevisionHydratorRegistry revisionHydratorRegistry;

    private DataQualityAssessmentRepository dataQualityAssessmentRepository;

    private DataQualityCalculator dataQualityCalculator;

    @BeforeAll
    static void loadProfiles() {
        new DataQualityAssessmentConfigurationLoader(mock(LanguageTagService.class), "");
    }

    @BeforeEach
    void setUp() {
        revisionHydratorRegistry = mock(RevisionHydratorRegistry.class);
        dataQualityAssessmentRepository = mock(DataQualityAssessmentRepository.class);

        dataQualityCalculator = new DataQualityCalculator(
            revisionHydratorRegistry,
            mock(DocumentRepository.class),
            mock(PersonRepository.class),
            mock(OrganisationUnitRepository.class),
            mock(OrganisationUnitIndexRepository.class),
            mock(CountryRepository.class),
            mock(LanguageRepository.class),
            mock(PublisherRepository.class),
            mock(RestTemplateProvider.class),
            mock(DataQualityAssessmentIndexer.class),
            List.of(),
            List.of(),
            mock(InvolvementRepository.class)
        );
    }

    @Test
    public void shouldNotReportMissingContributorsForProceedings() {
        var proceedings = new ProceedingsResponseDTO();

        var assessment = assess(proceedings, DocumentPublicationType.PROCEEDINGS);

        assertFalse(hasIssue(assessment, CONTRIBUTORS_MISSING));
    }

    @Test
    public void shouldNotReportMissingContributorsForEditedBook() {
        var monograph = new MonographDTO();
        monograph.setMonographType(MonographType.EDITED_BOOK);

        var assessment = assess(monograph, DocumentPublicationType.MONOGRAPH);

        assertFalse(hasIssue(assessment, CONTRIBUTORS_MISSING));
    }

    @Test
    public void shouldReportMissingContributorsForResearchMonograph() {
        var monograph = new MonographDTO();
        monograph.setMonographType(MonographType.RESEARCH_MONOGRAPH);

        var assessment = assess(monograph, DocumentPublicationType.MONOGRAPH);

        assertTrue(hasIssue(assessment, CONTRIBUTORS_MISSING));
    }

    @Test
    public void shouldNotReportMissingManagedContributorForEditedBook() {
        var monograph = new MonographDTO();
        monograph.setMonographType(MonographType.EDITED_BOOK);
        monograph.setContributions(List.of(unmanagedContribution()));

        var assessment = assess(monograph, DocumentPublicationType.MONOGRAPH);

        assertFalse(hasIssue(assessment, NO_MANAGED_CONTRIBUTOR));
        assertFalse(hasIssue(assessment, CONTRIBUTORS_MISSING));
    }

    @Test
    public void shouldReportMissingManagedContributorForResearchMonograph() {
        var monograph = new MonographDTO();
        monograph.setMonographType(MonographType.RESEARCH_MONOGRAPH);
        monograph.setContributions(List.of(unmanagedContribution()));

        var assessment = assess(monograph, DocumentPublicationType.MONOGRAPH);

        assertTrue(hasIssue(assessment, NO_MANAGED_CONTRIBUTOR));
        assertFalse(hasIssue(assessment, CONTRIBUTORS_MISSING));
    }

    private PersonDocumentContributionDTO unmanagedContribution() {
        var contribution = new PersonDocumentContributionDTO();
        contribution.setPersonId(null);
        return contribution;
    }

    private DataQualityAssessment assess(DocumentDTO dto,
                                         DocumentPublicationType entityType) {
        var objectMapper = ObjectMapperProvider.provideObjectmapper();

        when(revisionHydratorRegistry.getAssessmentDtoClass(entityType.name()))
            .thenAnswer(invocation -> dto.getClass());

        var assessment = DataQualityAssessment.builder()
            .revision(EntityRevision.builder().entityType(entityType.name()).entityId(1).build())
            .profileName(PROFILE)
            .profileVersion(VERSION)
            .startedAt(Instant.now())
            .activitiesCount(0)
            .build();

        try {
            var assessed = dataQualityCalculator.assessDataQuality(
                assessment,
                objectMapper.writeValueAsString(dto),
                objectMapper,
                dataQualityAssessmentRepository,
                TARGETS
            );

            assertTrue(assessed);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return assessment;
    }

    private boolean hasIssue(DataQualityAssessment assessment, String issueKey) {
        return assessment.getIssues().stream()
            .anyMatch(issue -> issueKey.equals(issue.getKey()));
    }
}
