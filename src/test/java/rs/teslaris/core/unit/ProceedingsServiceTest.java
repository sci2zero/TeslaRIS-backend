package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
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
    public void shouldReturnProceedingsWhenProceedingsExists() {
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
    public void shouldReturnProceedingsForEvent() {
        // given
        var event = new Conference();
        var document = new Proceedings();
        document.setDocumentDate("MOCK DATE");
        document.setEvent(event);

        when(proceedingsRepository.findProceedingsForEventId(1)).thenReturn(List.of(document));

        // when
        var actualProceedings = proceedingsService.readProceedingsForEventId(1);

        // then
        assertEquals(1, actualProceedings.size());
    }

    @Test
    public void shouldCreateProceedings() {
        // Given
        var proceedingsDTO = new ProceedingsDTO();
        proceedingsDTO.setLanguageTagIds(new ArrayList<>());
        var document = new Proceedings();
        document.setDocumentDate("MOCK DATE");
        document.setEvent(new Conference());

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(proceedingJPAService.save(any())).thenReturn(document);

        // When
        var result = proceedingsService.createProceedings(proceedingsDTO, true);

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

        when(proceedingJPAService.findOne(proceedingsId)).thenReturn(proceedingsToUpdate);

        // When
        proceedingsService.updateProceedings(proceedingsId, proceedingsDTO);

        // Then
        verify(proceedingJPAService).findOne(eq(proceedingsId));
        verify(personContributionService).setPersonDocumentContributionsForDocument(
            eq(proceedingsToUpdate), eq(proceedingsDTO));
    }

    @Test
    public void shouldReturnProceedingsWhenOldIdExists() {
        // Given
        var proceedingsId = 123;
        var expected = new Proceedings();
        when(documentRepository.findDocumentByOldId(proceedingsId)).thenReturn(
            Optional.of(expected));

        // When
        var actual = proceedingsService.findDocumentByOldId(proceedingsId);

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void shouldReturnNullWhenProceedingsDoesNotExist() {
        // Given
        var proceedingsId = 123;
        when(documentRepository.findDocumentByOldId(proceedingsId)).thenReturn(Optional.empty());

        // When
        var actual = proceedingsService.findDocumentByOldId(proceedingsId);

        // Then
        assertNull(actual);
    }

    @Test
    void findProceedingsForBookSeries_ReturnsPageOfProceedings() {
        // Given
        var bookSeriesId = 123;
        var pageable = Pageable.ofSize(10).withPage(0);

        when(documentPublicationIndexRepository.findByTypeAndJournalId(
            DocumentPublicationType.PROCEEDINGS.name(), bookSeriesId, pageable))
            .thenReturn(new PageImpl<>(
                List.of(new DocumentPublicationIndex(), new DocumentPublicationIndex())));

        // When
        var result = proceedingsService.findProceedingsForBookSeries(bookSeriesId, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getSize() >= 2);
    }
}
