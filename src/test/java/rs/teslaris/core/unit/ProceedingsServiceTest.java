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
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ProceedingsRepository;
import rs.teslaris.core.service.impl.document.ProceedingsServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.ProceedingJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

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

    @Mock
    private ProceedingJPAServiceImpl proceedingJPAService;

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

        when(proceedingJPAService.findOne(1)).thenReturn(expectedProceedings);

        // when
        var actualProceedings = proceedingsService.findProceedingsById(1);

        // then
        assertEquals(expectedProceedings, actualProceedings);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenProceedingsDoesNotExist() {
        // given
        when(proceedingJPAService.findOne(1)).thenThrow(NotFoundException.class);

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
        when(proceedingJPAService.save(any())).thenReturn(document);

        // When
        var result = proceedingsService.createProceedings(proceedingsDTO);

        // Then
        verify(multilingualContentService, times(4)).getMultilingualContent(any());
        verify(personContributionService).setPersonDocumentContributionsForDocument(eq(document),
            eq(proceedingsDTO));
        verify(proceedingJPAService).save(eq(document));
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

        when(proceedingJPAService.findOne(proceedingsId)).thenReturn(proceedingsToUpdate);

        // When
        proceedingsService.updateProceedings(proceedingsId, proceedingsDTO);

        // Then
        verify(proceedingJPAService).findOne(eq(proceedingsId));
        verify(personContributionService).setPersonDocumentContributionsForDocument(
            eq(proceedingsToUpdate), eq(proceedingsDTO));
    }
}
