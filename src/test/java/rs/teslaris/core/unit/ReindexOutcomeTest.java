package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.impl.commontypes.ReindexServiceImpl;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.GeneticMaterialService;
import rs.teslaris.core.service.interfaces.document.IntangibleProductService;
import rs.teslaris.core.service.interfaces.document.IntellectualPropertyService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.MaterialProductService;
import rs.teslaris.core.service.interfaces.document.MonographPublicationService;
import rs.teslaris.core.service.interfaces.document.MonographService;
import rs.teslaris.core.service.interfaces.document.PerformanceRelatedOutputService;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.project.service.impl.commontypes.ProjectReindexServiceImpl;
import rs.teslaris.project.service.interfaces.funding.FundingProgramService;

@ExtendWith(MockitoExtension.class)
class ReindexOutcomeTest {
    @Mock private PersonService personService;
    @Mock private DocumentPublicationService documentPublicationService;
    @Mock private ThesisService thesisService;
    @Mock private ProceedingsService proceedingsService;
    @Mock private JournalPublicationService journalPublicationService;
    @Mock private ProceedingsPublicationService proceedingsPublicationService;
    @Mock private IntellectualPropertyService intellectualPropertyService;
    @Mock private IntangibleProductService intangibleProductService;
    @Mock private MonographService monographService;
    @Mock private MonographPublicationService monographPublicationService;
    @Mock private MaterialProductService materialProductService;
    @Mock private GeneticMaterialService geneticMaterialService;
    @Mock private PerformanceRelatedOutputService performanceRelatedOutputService;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @Mock private FundingProgramService fundingProgramService;
    @InjectMocks private ReindexServiceImpl reindexService;
    @InjectMocks private ProjectReindexServiceImpl projectReindexService;

    @Test
    void shouldPropagateAsynchronousCoreIndexFailure() {
        when(personService.reindexPersons()).thenReturn(
            CompletableFuture.failedFuture(new IllegalStateException("Person index unavailable")));
        var failure = assertThrows(CompletionException.class, () ->
            reindexService.reindexDatabase(List.of(EntityType.PERSON), false, null));
        assertTrue(failure.getMessage().contains("Person index unavailable"));
    }

    @Test
    void shouldReportPublicationFailureWhileStillAttemptingRemainingTypes() {
        doThrow(new IllegalStateException("Thesis index unavailable")).when(thesisService)
            .reindexTheses();
        var failure = assertThrows(CompletionException.class, () ->
            reindexService.reindexDatabase(List.of(EntityType.PUBLICATION), false, null));
        assertTrue(failure.getMessage().contains("Error reindexing theses"));
        assertTrue(failure.getMessage().contains("Thesis index unavailable"));
        verify(journalPublicationService).reindexJournalPublications();
        verify(performanceRelatedOutputService).reindexPerformanceRelatedOutputs();
    }

    @Test
    void shouldPropagateProjectIndexFailure() {
        when(fundingProgramService.reindexFundingPrograms()).thenReturn(
            CompletableFuture.failedFuture(new IllegalStateException("Funding index unavailable")));
        var failure = assertThrows(CompletionException.class, () ->
            projectReindexService.reindexDatabase(List.of(EntityType.FUNDING_PROGRAM)));
        assertTrue(failure.getMessage().contains("Funding index unavailable"));
    }
}
