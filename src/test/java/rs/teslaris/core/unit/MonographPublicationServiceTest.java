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
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.MonographPublicationType;
import rs.teslaris.core.model.document.MonographType;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.MonographPublicationRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.MonographPublicationServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.MonographPublicationJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.MonographService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class MonographPublicationServiceTest {

    @Mock
    private MonographPublicationJPAServiceImpl monographPublicationJPAService;

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
    private MonographPublicationRepository monographPublicationRepository;

    @Mock
    private MonographService monographService;

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private OrganisationUnitTrustConfigurationService organisationUnitTrustConfigurationService;

    @Mock
    private CitationService citationService;

    @InjectMocks
    private MonographPublicationServiceImpl monographPublicationService;


    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(monographPublicationService, "documentApprovedByDefault",
            true);

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void shouldReadMonographPublicationByIdWhenMonographPublicationIsApproved() {
        // Given
        var monographPublicationId = 1;
        var monographPublication = new MonographPublication();
        monographPublication.setId(monographPublicationId);
        monographPublication.setApproveStatus(ApproveStatus.APPROVED);
        monographPublication.setMonograph(new Monograph());

        when(monographPublicationJPAService.findOne(monographPublicationId)).thenReturn(
            monographPublication);

        // When
        var monographPublicationDTO =
            monographPublicationService.readMonographPublicationById(monographPublicationId);

        // Then
        assertNotNull(monographPublicationDTO);
        verify(monographPublicationJPAService, times(1)).findOne(monographPublicationId);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenMonographPublicationIsDeclined() {
        // Given
        var monographPublicationId = 1;
        var monographPublication = new MonographPublication();
        monographPublication.setId(monographPublicationId);
        monographPublication.setApproveStatus(ApproveStatus.DECLINED);

        when(monographPublicationJPAService.findOne(monographPublicationId)).thenReturn(
            monographPublication);

        // When
        assertThrows(NotFoundException.class,
            () -> monographPublicationService.readMonographPublicationById(monographPublicationId));

        // Then (NotFoundException should be thrown)
        verify(monographPublicationJPAService, times(1)).findOne(monographPublicationId);
    }

    @Test
    void shouldCreateAndApproveMonographPublicationWhenDocumentApprovedByDefault() {
        // Given
        var monographPublicationDTO = new MonographPublicationDTO();
        monographPublicationDTO.setMonographPublicationType(MonographPublicationType.CHAPTER);
        var newMonographPublication = new MonographPublication();
        newMonographPublication.setMonograph(new Monograph() {{
            setId(1);
        }});
        newMonographPublication.setApproveStatus(ApproveStatus.APPROVED);

        when(monographService.findMonographById(any())).thenReturn(new Monograph() {{
            setMonographType(MonographType.BOOK);
        }});
        when(monographPublicationJPAService.save(any(MonographPublication.class))).thenReturn(
            newMonographPublication);

        // When
        var result =
            monographPublicationService.createMonographPublication(monographPublicationDTO, true);

        // Then
        assertNotNull(result);
        assertEquals(ApproveStatus.APPROVED, result.getApproveStatus());
        verify(monographPublicationJPAService, times(1)).save(any(MonographPublication.class));
    }

    @Test
    void shouldCreateAndRequestMonographPublicationWhenDocumentNotApprovedByDefault() {
        // Given
        var monographPublicationDTO = new MonographPublicationDTO();
        monographPublicationDTO.setMonographPublicationType(MonographPublicationType.CHAPTER);
        var newMonographPublication = new MonographPublication();
        newMonographPublication.setMonograph(new Monograph() {{
            setId(1);
        }});
        newMonographPublication.setApproveStatus(ApproveStatus.REQUESTED);

        when(monographService.findMonographById(any())).thenReturn(new Monograph() {{
            setMonographType(MonographType.BOOK);
        }});
        when(monographPublicationJPAService.save(any(MonographPublication.class))).thenReturn(
            newMonographPublication);

        ReflectionTestUtils.setField(monographPublicationService, "documentApprovedByDefault",
            false);

        // When
        var result =
            monographPublicationService.createMonographPublication(monographPublicationDTO, true);

        // Then
        assertNotNull(result);
        assertEquals(ApproveStatus.REQUESTED, result.getApproveStatus());
        verify(monographPublicationJPAService, times(1)).save(any(MonographPublication.class));
    }

    @Test
    void shouldNotIndexMonographPublicationWhenIndexIsFalse() {
        // Given
        var monographPublicationDTO = new MonographPublicationDTO();
        monographPublicationDTO.setMonographPublicationType(MonographPublicationType.CHAPTER);
        var newMonographPublication = new MonographPublication();
        newMonographPublication.setApproveStatus(ApproveStatus.APPROVED);

        when(monographService.findMonographById(any())).thenReturn(new Monograph() {{
            setMonographType(MonographType.BOOK);
        }});
        when(monographPublicationJPAService.save(any(MonographPublication.class))).thenReturn(
            newMonographPublication);

        // When
        var result =
            monographPublicationService.createMonographPublication(monographPublicationDTO, false);

        // Then
        assertNotNull(result);
        assertEquals(ApproveStatus.APPROVED, result.getApproveStatus());
        verify(monographPublicationJPAService, times(1)).save(any(MonographPublication.class));
    }

    @Test
    void shouldUpdateMonographPublicationAndIndexWhenApproved() {
        // Given
        var monographPublicationId = 1;
        var monographPublicationDTO = new MonographPublicationDTO();
        monographPublicationDTO.setMonographId(1);
        monographPublicationDTO.setMonographPublicationType(MonographPublicationType.CHAPTER);
        var monographPublicationToUpdate = new MonographPublication();
        monographPublicationToUpdate.setId(monographPublicationId);
        monographPublicationToUpdate.setApproveStatus(ApproveStatus.APPROVED);
        monographPublicationToUpdate.setMonograph(new Monograph() {{
            setId(2);
        }});

        when(monographPublicationJPAService.findOne(monographPublicationId)).thenReturn(
            monographPublicationToUpdate);
        when(monographService.findMonographById(anyInt())).thenReturn(new Monograph() {{
            setMonographType(MonographType.BOOK);
            setId(3);
        }});
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            monographPublicationId)).thenReturn(Optional.of(new DocumentPublicationIndex()));

        // When
        monographPublicationService.editMonographPublication(monographPublicationId,
            monographPublicationDTO);

        // Then
        verify(monographPublicationJPAService, times(1)).findOne(monographPublicationId);
        verify(monographPublicationJPAService, times(1)).save(monographPublicationToUpdate);
    }

    @Test
    void shouldUpdateMonographPublicationAndNotIndexWhenNotApproved() {
        // Given
        var monographPublicationId = 1;
        var monographPublicationDTO = new MonographPublicationDTO();
        monographPublicationDTO.setMonographPublicationType(MonographPublicationType.PREFACE);
        var monographPublicationToUpdate = new MonographPublication();
        monographPublicationToUpdate.setId(monographPublicationId);
        monographPublicationToUpdate.setApproveStatus(ApproveStatus.REQUESTED);
        monographPublicationToUpdate.setMonograph(new Monograph() {{
            setId(2);
        }});

        when(monographService.findMonographById(any())).thenReturn(new Monograph() {{
            setMonographType(MonographType.BOOK);
            setId(3);
        }});
        when(monographPublicationJPAService.findOne(monographPublicationId)).thenReturn(
            monographPublicationToUpdate);
        when(monographPublicationJPAService.save(any(MonographPublication.class))).thenReturn(
            monographPublicationToUpdate);

        // When
        monographPublicationService.editMonographPublication(monographPublicationId,
            monographPublicationDTO);

        // Then
        verify(monographPublicationJPAService, times(1)).findOne(monographPublicationId);
        verify(monographPublicationJPAService, times(1)).save(monographPublicationToUpdate);
    }

    @Test
    void shouldDeleteMonographPublicationAndIndexWhenApproved() {
        // Given
        var monographPublicationId = 1;
        var monographPublicationToDelete = new MonographPublication();
        monographPublicationToDelete.setId(monographPublicationId);
        monographPublicationToDelete.setApproveStatus(ApproveStatus.APPROVED);

        var monographPublicationIndex = new DocumentPublicationIndex();

        when(monographPublicationJPAService.findOne(monographPublicationId)).thenReturn(
            monographPublicationToDelete);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            monographPublicationId)).thenReturn(Optional.of(monographPublicationIndex));

        // When
        monographPublicationService.deleteMonographPublication(monographPublicationId);

        // Then
        verify(monographPublicationJPAService, times(1)).findOne(monographPublicationId);
        verify(monographPublicationJPAService, times(1)).delete(monographPublicationId);
        verify(documentPublicationIndexRepository, times(1)).delete(monographPublicationIndex);
    }

    @Test
    void shouldDeleteMonographPublicationWhenNotApproved() {
        // Given
        var monographPublicationId = 1;
        var monographPublicationToDelete = new MonographPublication();
        monographPublicationToDelete.setId(monographPublicationId);
        monographPublicationToDelete.setApproveStatus(ApproveStatus.REQUESTED);

        when(monographPublicationJPAService.findOne(monographPublicationId)).thenReturn(
            monographPublicationToDelete);

        // When
        monographPublicationService.deleteMonographPublication(monographPublicationId);

        // Then
        verify(monographPublicationJPAService, times(1)).findOne(monographPublicationId);
        verify(monographPublicationJPAService, times(1)).delete(monographPublicationId);
        verify(documentPublicationIndexRepository, times(1)).delete(
            any(DocumentPublicationIndex.class));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenMonographPublicationNotFound() {
        // Given
        var monographPublicationId = 1;

        when(monographPublicationJPAService.findOne(monographPublicationId)).thenThrow(
            NotFoundException.class);

        // When
        assertThrows(NotFoundException.class,
            () -> monographPublicationService.deleteMonographPublication(monographPublicationId));

        // Then
        verify(monographPublicationJPAService, times(1)).findOne(monographPublicationId);
        verify(monographPublicationJPAService, times(0)).delete(anyInt());
        verify(documentPublicationIndexRepository, times(0)).delete(
            any(DocumentPublicationIndex.class));
    }

    @Test
    public void shouldReindexMonographPublications() {
        // Given
        var monographPublication1 = new MonographPublication();
        monographPublication1.setMonograph(new Monograph() {{
            setId(1);
        }});
        var monographPublication2 = new MonographPublication();
        monographPublication2.setMonograph(new Monograph() {{
            setId(2);
        }});
        var monographPublication3 = new MonographPublication();
        monographPublication3.setMonograph(new Monograph() {{
            setId(3);
        }});
        var monographPublications =
            Arrays.asList(monographPublication1, monographPublication2, monographPublication3);
        var page1 =
            new PageImpl<>(monographPublications.subList(0, 2), PageRequest.of(0, 10),
                monographPublications.size());
        var page2 =
            new PageImpl<>(monographPublications.subList(2, 3), PageRequest.of(1, 10),
                monographPublications.size());

        when(monographPublicationJPAService.findAll(any(PageRequest.class))).thenReturn(page1,
            page2);

        // When
        monographPublicationService.reindexMonographPublications();

        // Then
        verify(monographPublicationJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }


    @Test
    public void shouldFindAllPublicationsForMonograph() {
        // Given
        var monographId = 1;
        var pageable = PageRequest.of(0, 10);
        var mockPage = new PageImpl<>(List.of(new DocumentPublicationIndex()));

        when(documentPublicationIndexRepository.findByTypeAndMonographIdAndIsApprovedTrue(
            DocumentPublicationType.MONOGRAPH_PUBLICATION.name(), monographId, pageable))
            .thenReturn(mockPage);

        // When
        var result =
            monographPublicationService.findAllPublicationsForMonograph(monographId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(documentPublicationIndexRepository, times(1))
            .findByTypeAndMonographIdAndIsApprovedTrue(
                DocumentPublicationType.MONOGRAPH_PUBLICATION.name(), monographId, pageable);
    }

    @Test
    public void shouldFindAuthorsPublicationsForMonograph() {
        // Given
        var monographId = 1;
        var authorId = 2;
        var mockPublications = List.of(new DocumentPublicationIndex());

        when(documentPublicationIndexRepository.findByTypeAndMonographIdAndAuthorIds(
            DocumentPublicationType.MONOGRAPH_PUBLICATION.name(), monographId, authorId))
            .thenReturn(mockPublications);

        // When
        var result = monographPublicationService
            .findAuthorsPublicationsForMonograph(monographId, authorId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(documentPublicationIndexRepository, times(1))
            .findByTypeAndMonographIdAndAuthorIds(
                DocumentPublicationType.MONOGRAPH_PUBLICATION.name(), monographId, authorId);
    }

    @Test
    void shouldThrowNotFoundWhenMonographPublicationDoesNotExist() {
        // Given
        var oldId = 123;
        when(monographPublicationRepository.findMonographPublicationByOldIdsContains(
            oldId)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class,
            () -> monographPublicationService.readMonographPublicationByOldId(oldId));
        verify(monographPublicationRepository).findMonographPublicationByOldIdsContains(oldId);
    }

    @Test
    void shouldThrowNotFoundWhenMonographPublicationIsNotApproved() {
        // Given
        var oldId = 456;
        var monographPublication = new MonographPublication();
        monographPublication.setApproveStatus(ApproveStatus.REQUESTED);
        when(monographPublicationRepository.findMonographPublicationByOldIdsContains(
            oldId)).thenReturn(Optional.of(monographPublication));

        // When / Then
        assertThrows(NotFoundException.class,
            () -> monographPublicationService.readMonographPublicationByOldId(oldId));
        verify(monographPublicationRepository).findMonographPublicationByOldIdsContains(oldId);
    }

    @Test
    void shouldReturnDtoWhenMonographPublicationIsApproved() {
        // Given
        var oldId = 789;
        var monographPublication = new MonographPublication();
        monographPublication.setApproveStatus(ApproveStatus.APPROVED);
        monographPublication.setMonograph(new Monograph());

        when(monographPublicationRepository.findMonographPublicationByOldIdsContains(
            oldId)).thenReturn(Optional.of(monographPublication));

        // When
        var result = monographPublicationService.readMonographPublicationByOldId(oldId);

        // Then
        assertNotNull(result);
        verify(monographPublicationRepository).findMonographPublicationByOldIdsContains(oldId);
    }
}

