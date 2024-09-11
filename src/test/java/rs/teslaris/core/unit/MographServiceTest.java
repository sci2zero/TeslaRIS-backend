package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.MonographRepository;
import rs.teslaris.core.service.impl.document.MonographServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.MonographJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class MographServiceTest {

    @Mock
    private MonographJPAServiceImpl monographJPAService;

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private JournalService journalService;

    @Mock
    private BookSeriesService bookSeriesService;

    @Mock
    private ResearchAreaService researchAreaService;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private MonographRepository monographRepository;

    @Mock
    private SearchService<DocumentPublicationIndex> searchService;

    @InjectMocks
    private MonographServiceImpl monographService;


    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(monographService, "documentApprovedByDefault", true);

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void shouldReadMonographByIdWhenMonographIsApproved() {
        // Given
        var monographId = 1;
        var monograph = new Monograph();
        monograph.setId(monographId);
        monograph.setApproveStatus(ApproveStatus.APPROVED);

        when(monographJPAService.findOne(monographId)).thenReturn(monograph);

        // When
        var monographDTO = monographService.readMonographById(monographId);

        // Then
        assertNotNull(monographDTO);
        verify(monographJPAService, times(1)).findOne(monographId);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenMonographIsDeclined() {
        // Given
        var monographId = 1;
        var monograph = new Monograph();
        monograph.setId(monographId);
        monograph.setApproveStatus(ApproveStatus.DECLINED);

        when(monographJPAService.findOne(monographId)).thenReturn(monograph);

        // When
        assertThrows(NotFoundException.class,
            () -> monographService.readMonographById(monographId));

        // Then (NotFoundException should be thrown)
        verify(monographJPAService, times(1)).findOne(monographId);
    }

    @Test
    void shouldCreateAndApproveMonographWhenDocumentApprovedByDefault() {
        // Given
        var monographDTO = new MonographDTO();
        monographDTO.setLanguageTagIds(new ArrayList<>());
        var newMonograph = new Monograph();
        newMonograph.setApproveStatus(ApproveStatus.APPROVED);

        when(monographJPAService.save(any(Monograph.class))).thenReturn(newMonograph);

        // When
        var result = monographService.createMonograph(monographDTO, true);

        // Then
        assertNotNull(result);
        assertEquals(ApproveStatus.APPROVED, result.getApproveStatus());
        verify(monographJPAService, times(1)).save(any(Monograph.class));
    }

    @Test
    void shouldCreateAndRequestMonographWhenDocumentNotApprovedByDefault() {
        // Given
        var monographDTO = new MonographDTO();
        monographDTO.setLanguageTagIds(new ArrayList<>());
        var newMonograph = new Monograph();
        newMonograph.setApproveStatus(ApproveStatus.REQUESTED);

        when(monographJPAService.save(any(Monograph.class))).thenReturn(newMonograph);

        ReflectionTestUtils.setField(monographService, "documentApprovedByDefault", false);

        // When
        var result = monographService.createMonograph(monographDTO, true);

        // Then
        assertNotNull(result);
        assertEquals(ApproveStatus.REQUESTED, result.getApproveStatus());
        verify(monographJPAService, times(1)).save(any(Monograph.class));
    }

    @Test
    void shouldNotIndexMonographWhenIndexIsFalse() {
        // Given
        var monographDTO = new MonographDTO();
        monographDTO.setLanguageTagIds(new ArrayList<>());
        var newMonograph = new Monograph();
        newMonograph.setApproveStatus(ApproveStatus.APPROVED);

        when(monographJPAService.save(any(Monograph.class))).thenReturn(newMonograph);

        // When
        var result = monographService.createMonograph(monographDTO, false);

        // Then
        assertNotNull(result);
        assertEquals(ApproveStatus.APPROVED, result.getApproveStatus());
        verify(monographJPAService, times(1)).save(any(Monograph.class));
    }

    @Test
    void shouldUpdateMonographAndIndexWhenApproved() {
        // Given
        var monographId = 1;
        var monographDTO = new MonographDTO();
        monographDTO.setLanguageTagIds(new ArrayList<>());
        var monographToUpdate = new Monograph();
        monographToUpdate.setId(monographId);
        monographToUpdate.setApproveStatus(ApproveStatus.APPROVED);

        when(monographJPAService.findOne(monographId)).thenReturn(monographToUpdate);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            monographId)).thenReturn(Optional.of(new DocumentPublicationIndex()));

        // When
        monographService.editMonograph(monographId, monographDTO);

        // Then
        verify(monographJPAService, times(1)).findOne(monographId);
        verify(monographJPAService, times(1)).save(monographToUpdate);
    }

    @Test
    void shouldUpdateMonographAndNotIndexWhenNotApproved() {
        // Given
        var monographId = 1;
        var monographDTO = new MonographDTO();
        monographDTO.setLanguageTagIds(new ArrayList<>());
        var monographToUpdate = new Monograph();
        monographToUpdate.setId(monographId);
        monographToUpdate.setApproveStatus(ApproveStatus.REQUESTED);

        when(monographJPAService.findOne(monographId)).thenReturn(monographToUpdate);

        // When
        monographService.editMonograph(monographId, monographDTO);

        // Then
        verify(monographJPAService, times(1)).findOne(monographId);
        verify(monographJPAService, times(1)).save(monographToUpdate);
    }

    @Test
    void shouldDeleteMonographAndIndexWhenApproved() {
        // Given
        var monographId = 1;
        var monographToDelete = new Monograph();
        monographToDelete.setId(monographId);
        monographToDelete.setApproveStatus(ApproveStatus.APPROVED);

        var monographIndex = new DocumentPublicationIndex();

        when(monographJPAService.findOne(monographId)).thenReturn(monographToDelete);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            monographId)).thenReturn(Optional.of(monographIndex));

        // When
        monographService.deleteMonograph(monographId);

        // Then
        verify(monographJPAService, times(1)).findOne(monographId);
        verify(monographJPAService, times(1)).delete(monographId);
        verify(documentPublicationIndexRepository, times(1)).delete(monographIndex);
    }

    @Test
    void shouldDeleteMonographWhenNotApproved() {
        // Given
        var monographId = 1;
        var monographToDelete = new Monograph();
        monographToDelete.setId(monographId);
        monographToDelete.setApproveStatus(ApproveStatus.REQUESTED);

        when(monographJPAService.findOne(monographId)).thenReturn(monographToDelete);

        // When
        monographService.deleteMonograph(monographId);

        // Then
        verify(monographJPAService, times(1)).findOne(monographId);
        verify(monographJPAService, times(1)).delete(monographId);
        verify(documentPublicationIndexRepository, times(0)).delete(
            any(DocumentPublicationIndex.class));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenMonographNotFound() {
        // Given
        var monographId = 1;

        when(monographJPAService.findOne(monographId)).thenThrow(NotFoundException.class);

        // When
        assertThrows(NotFoundException.class, () -> monographService.deleteMonograph(monographId));

        // Then
        verify(monographJPAService, times(1)).findOne(monographId);
        verify(monographJPAService, times(0)).delete(anyInt());
        verify(documentPublicationIndexRepository, times(0)).delete(
            any(DocumentPublicationIndex.class));
    }

    @Test
    public void shouldReindexMonographs() {
        // Given
        var monograph1 = new Monograph();
        var monograph2 = new Monograph();
        var monograph3 = new Monograph();
        var monographs = Arrays.asList(monograph1, monograph2, monograph3);
        var page1 =
            new PageImpl<>(monographs.subList(0, 2), PageRequest.of(0, 10), monographs.size());
        var page2 =
            new PageImpl<>(monographs.subList(2, 3), PageRequest.of(1, 10), monographs.size());

        when(monographJPAService.findAll(any(PageRequest.class))).thenReturn(page1, page2);

        // When
        monographService.reindexMonographs();

        // Then
        verify(monographJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }

    @Test
    public void shouldFindMonographsWhenSearchingWithSimpleQuery() {
        // given
        var tokens = Arrays.asList("ključna", "ријеч", "keyword");

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(
                List.of(new DocumentPublicationIndex(), new DocumentPublicationIndex())));

        // when
        var result =
            monographService.searchMonographs(new ArrayList<>(tokens));

        // then
        assertEquals(result.getTotalElements(), 2L);
    }
}
