package rs.teslaris.core.unit.project;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.dto.project.ProjectDocumentDTO;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.model.project.ProjectDocument;
import rs.teslaris.project.model.project.ProjectDocumentType;
import rs.teslaris.project.repository.project.ProjectDocumentRepository;
import rs.teslaris.project.service.impl.project.ProjectDocumentServiceImpl;
import rs.teslaris.project.service.interfaces.project.ProjectService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ProjectDocumentServiceTest {

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private ProjectService projectService;

    @Mock
    private DocumentPublicationService documentPublicationService;

    @Mock
    private ProjectDocumentRepository projectDocumentRepository;

    @Mock
    private IndexBulkUpdateService indexBulkUpdateService;

    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private ProjectDocumentServiceImpl projectDocumentService;

    @Test
    public void shouldCreateProjectDocumentSuccessfully() {
        // given
        var dto = new ProjectDocumentDTO();
        dto.setProjectId(1);
        dto.setDocumentId(2);
        dto.setRelationType(ProjectDocumentType.RESULT);
        dto.setTextualDescription(List.of());
        dto.setFundingParts(List.of());

        var project = new Project();
        project.setId(1);

        // used Proceedings class because the Document is abstract and could not be instantiated
        var document = new Proceedings();
        document.setId(2);

        var savedDocument = new ProjectDocument();
        savedDocument.setId(10);
        savedDocument.setProject(project);
        savedDocument.setDocument(document);

        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(Set.of());
        when(projectService.findOne(1)).thenReturn(project);
        when(documentPublicationService.findOne(2)).thenReturn(document);
        when(projectDocumentRepository.save(any(ProjectDocument.class))).thenReturn(savedDocument);

        // when
        var result = projectDocumentService.createProjectDocument(dto);

        // then
        assertNotNull(result);
        assertEquals(10, result.getId());
        verify(projectService).findOne(1);
        verify(documentPublicationService).findOne(2);
        verify(projectDocumentRepository).save(any(ProjectDocument.class));
        verify(indexBulkUpdateService).setIdFieldForRecord("document_publication", "databaseId", 2, "project_id", 1);
    }

    @Test
    public void shouldCreateProjectDocumentWithFundingParts() {
        // given
        var monetaryAmountDTO = new MonetaryAmountDTO();
        monetaryAmountDTO.setCurrencyId(1);
        monetaryAmountDTO.setAmount(5000.0);

        var fundingPartDTO = new FundingPartDTO();
        fundingPartDTO.setFundingId(100);
        fundingPartDTO.setDescription(List.of());
        fundingPartDTO.setAmount(monetaryAmountDTO);

        var dto = new ProjectDocumentDTO();
        dto.setProjectId(1);
        dto.setDocumentId(2);
        dto.setRelationType(ProjectDocumentType.RESULT);
        dto.setTextualDescription(List.of());
        dto.setFundingParts(List.of(fundingPartDTO));

        var project = new Project();
        project.setId(1);

        // used Proceedings class because the Document is abstract and could not be instantiated
        var document = new Proceedings();
        document.setId(2);

        var savedDocument = new ProjectDocument();
        savedDocument.setId(10);
        savedDocument.setProject(project);
        savedDocument.setDocument(document);

        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(Set.of());
        when(projectService.findOne(1)).thenReturn(project);
        when(documentPublicationService.findOne(2)).thenReturn(document);
        when(currencyService.findOne(1)).thenReturn(null);
        when(projectDocumentRepository.save(any(ProjectDocument.class))).thenReturn(savedDocument);

        // when
        var result = projectDocumentService.createProjectDocument(dto);

        // then
        assertNotNull(result);
        verify(currencyService).findOne(1);
        verify(multilingualContentService, times(2)).getMultilingualContent(anyList());
        verify(projectDocumentRepository).save(any(ProjectDocument.class));
    }

    @Test
    public void shouldThrowWhenProjectNotFound() {
        // given
        var dto = new ProjectDocumentDTO();
        dto.setProjectId(999);
        dto.setDocumentId(2);
        dto.setRelationType(ProjectDocumentType.RESULT);
        dto.setTextualDescription(List.of());
        dto.setFundingParts(List.of());

        when(projectService.findOne(999)).thenThrow(NotFoundException.class);

        // when & then
        assertThrows(NotFoundException.class, () -> projectDocumentService.createProjectDocument(dto));

        verify(projectDocumentRepository, never()).save(any());
    }

    @Test
    public void shouldThrowWhenDocumentNotFound() {
        // given
        var dto = new ProjectDocumentDTO();
        dto.setProjectId(1);
        dto.setDocumentId(999);
        dto.setRelationType(ProjectDocumentType.RESULT);
        dto.setTextualDescription(List.of());
        dto.setFundingParts(List.of());

        var project = new Project();
        project.setId(1);

        when(projectService.findOne(1)).thenReturn(project);
        when(documentPublicationService.findOne(999)).thenThrow(NotFoundException.class);

        // when & then
        assertThrows(NotFoundException.class, () -> projectDocumentService.createProjectDocument(dto));

        verify(projectDocumentRepository, never()).save(any());
    }

    @Test
    public void shouldCallIndexBulkUpdateAfterSave() {
        // given
        var dto = new ProjectDocumentDTO();
        dto.setProjectId(1);
        dto.setDocumentId(2);
        dto.setRelationType(ProjectDocumentType.RESULT);
        dto.setTextualDescription(List.of());
        dto.setFundingParts(List.of());

        var project = new Project();
        project.setId(1);

        // used Proceedings class because the Document is abstract and could not be instantiated
        var document = new Proceedings();
        document.setId(2);

        var savedDocument = new ProjectDocument();
        savedDocument.setId(10);
        savedDocument.setProject(project);
        savedDocument.setDocument(document);

        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(Set.of());
        when(projectService.findOne(1)).thenReturn(project);
        when(documentPublicationService.findOne(2)).thenReturn(document);
        when(projectDocumentRepository.save(any(ProjectDocument.class))).thenReturn(savedDocument);

        // when
        projectDocumentService.createProjectDocument(dto);

        // then
        verify(indexBulkUpdateService).setIdFieldForRecord(
                "document_publication", "databaseId", 2, "project_id", 1);
    }

    @Test
    public void shouldDeleteProjectDocumentSuccessfully() {
        // given
        var project = new Project();
        project.setId(1);

        var document = new Proceedings();
        document.setId(2);

        var projectDocument = new ProjectDocument();
        projectDocument.setId(10);
        projectDocument.setProject(project);
        projectDocument.setDocument(document);

        when(projectDocumentRepository.findById(10)).thenReturn(Optional.of(projectDocument));

        // when
        projectDocumentService.deleteProjectDocument(10);

        // then
        verify(projectDocumentRepository).save(argThat(pd -> pd.getDeleted().equals(true)));
        verify(indexBulkUpdateService).removeIdFieldFromRecord(
                "document_publication", "databaseId", 2, "project_id", 1);
    }

    @Test
    public void shouldThrowWhenProjectDocumentNotFound() {
        // given
        when(projectDocumentRepository.findById(999)).thenThrow(NotFoundException.class);

        // when & then
        assertThrows(NotFoundException.class,
                () -> projectDocumentService.deleteProjectDocument(999));

        verify(projectDocumentRepository, never()).delete(any());
        verify(indexBulkUpdateService, never()).removeIdFieldFromRecord(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotCallElasticsearchWhenDeleteFails() {
        // given
        var project = new Project();
        project.setId(1);

        var document = new Proceedings();
        document.setId(2);

        var projectDocument = new ProjectDocument();
        projectDocument.setId(10);
        projectDocument.setProject(project);
        projectDocument.setDocument(document);

        when(projectDocumentRepository.findById(10)).thenReturn(Optional.of(projectDocument));
        when(projectDocumentRepository.save(any(ProjectDocument.class))).thenThrow(RuntimeException.class);

        // when & then
        assertThrows(RuntimeException.class,
                () -> projectDocumentService.deleteProjectDocument(10));

        verify(indexBulkUpdateService, never()).removeIdFieldFromRecord(any(), any(), any(), any(), any());
    }
}
