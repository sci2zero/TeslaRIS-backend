package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ProceedingsRepository;
import rs.teslaris.core.service.DocumentFileService;
import rs.teslaris.core.service.EventService;
import rs.teslaris.core.service.JournalService;
import rs.teslaris.core.service.LanguageTagService;
import rs.teslaris.core.service.MultilingualContentService;
import rs.teslaris.core.service.PersonContributionService;
import rs.teslaris.core.service.PublisherService;
import rs.teslaris.core.service.impl.ProceedingsServiceImpl;

@SpringBootTest
public class ProceedingsServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentFileService documentFileService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private ProceedingsRepository proceedingsRepository;

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private JournalService journalService;

    @Mock
    private EventService eventService;

    @Mock
    private PublisherService publisherService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @InjectMocks
    private ProceedingsServiceImpl proceedingsService;


    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(proceedingsService, "documentApprovedByDefault", true);
    }

    @Test
    public void shouldReturnLanguageWhenProceedingsExists() {
        // given
        var expectedProceedings = new Proceedings();

        when(proceedingsRepository.findById(1)).thenReturn(Optional.of(expectedProceedings));

        // when
        var actualProceedings = proceedingsService.findProceedingsById(1);

        // then
        assertEquals(expectedProceedings, actualProceedings);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenProceedingsDoesNotExist() {
        // given
        when(proceedingsRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> proceedingsService.findProceedingsById(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldCreateProceedings() {
        // Given
        var proceedingsDTO = new ProceedingsDTO();
        proceedingsDTO.setLanguageTagIds(new ArrayList<>());
        var document = new Proceedings();
        document.setDocumentDate("MOCK DATE");
        document.setFileItems(new HashSet<>());
        document.setEvent(new Conference());
        document.setLanguages(new HashSet<>());
        document.setTitle(new HashSet<>());
        document.setSubTitle(new HashSet<>());
        document.setDescription(new HashSet<>());
        document.setKeywords(new HashSet<>());
        document.setContributors(new HashSet<>());
        document.setUris(new HashSet<>());

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(proceedingsRepository.save(any())).thenReturn(document);

        // When
        var result = proceedingsService.createProceedings(proceedingsDTO);

        // Then
        verify(multilingualContentService, times(4)).getMultilingualContent(any());
        verify(personContributionService).setPersonDocumentContributionsForDocument(eq(document),
            eq(proceedingsDTO));
        verify(proceedingsRepository).save(eq(document));
    }

    @Test
    public void shouldEditProceedings() {
        // Given
        var proceedingsId = 1;
        var proceedingsDTO = new ProceedingsDTO();
        proceedingsDTO.setLanguageTagIds(new ArrayList<>());
        var proceedingsToUpdate = new Proceedings();
        proceedingsToUpdate.setApproveStatus(ApproveStatus.REQUESTED);
        proceedingsToUpdate.setLanguages(new HashSet<>());
        proceedingsToUpdate.setTitle(new HashSet<>());
        proceedingsToUpdate.setSubTitle(new HashSet<>());
        proceedingsToUpdate.setDescription(new HashSet<>());
        proceedingsToUpdate.setKeywords(new HashSet<>());
        proceedingsToUpdate.setContributors(new HashSet<>());
        proceedingsToUpdate.setUris(new HashSet<>());

        when(proceedingsRepository.findById(proceedingsId)).thenReturn(
            Optional.of(proceedingsToUpdate));

        // When
        proceedingsService.updateProceedings(proceedingsId, proceedingsDTO);

        // Then
        verify(proceedingsRepository).findById(eq(proceedingsId));
        verify(personContributionService).setPersonDocumentContributionsForDocument(
            eq(proceedingsToUpdate), eq(proceedingsDTO));
    }
}
