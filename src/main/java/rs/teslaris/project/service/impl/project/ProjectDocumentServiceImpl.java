package rs.teslaris.project.service.impl.project;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.dto.project.ProjectDocumentDTO;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.funding.FundingPart;
import rs.teslaris.project.model.project.ProjectDocument;
import rs.teslaris.project.repository.project.ProjectDocumentRepository;
import rs.teslaris.project.service.interfaces.project.ProjectDocumentService;
import rs.teslaris.project.service.interfaces.project.ProjectService;

import java.util.HashSet;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectDocumentServiceImpl extends JPAServiceImpl<ProjectDocument>
    implements ProjectDocumentService {

    private final ProjectDocumentRepository projectDocumentRepository;

    private final MultilingualContentService multilingualContentService;

    private final ProjectService projectService;

    private final DocumentPublicationService documentService;

    private final IndexBulkUpdateService indexBulkUpdateService;

    private final CurrencyService currencyService;

    @Override
    protected JpaRepository<ProjectDocument, Integer> getEntityRepository() {
        return projectDocumentRepository;
    }

    @Override
    @Transactional
    public ProjectDocument createProjectDocument(ProjectDocumentDTO projectDocumentDTO) {
        var newProjectDocument = new ProjectDocument();

        setCommonFields(newProjectDocument, projectDocumentDTO);

        var savedProjectDocument = save(newProjectDocument);

        indexBulkUpdateService.setIdFieldForRecord("document_publication", "databaseId",
                savedProjectDocument.getDocument().getId(), "project_id", savedProjectDocument.getProject().getId());

        return savedProjectDocument;
    }

    @Override
    @Transactional
    public void deleteProjectDocument(Integer projectDocumentId) {
        var projectDocument = findOne(projectDocumentId);

        var documentId = projectDocument.getDocument().getId();

        delete(projectDocumentId);

        indexBulkUpdateService.setIdFieldForRecord("document_publication", "databaseId",
                documentId, "project_id", null);

    }

    private void setCommonFields(ProjectDocument projectDocument, ProjectDocumentDTO dto) {

        buildFundingParts(projectDocument, dto);
        projectDocument.setTextualDescription(
                multilingualContentService.getMultilingualContent(dto.getTextualDescription()));

        projectDocument.setRelationType(dto.getRelationType());

        if (Objects.nonNull(dto.getProjectId())) {
            projectDocument.setProject(projectService.findOne(dto.getProjectId()));
        } else {
            projectDocument.setProject(null);
        }

        if (Objects.nonNull(dto.getDocumentId())) {
            projectDocument.setDocument(documentService.findOne(dto.getDocumentId()));
        } else {
            projectDocument.setDocument(null);
        }
    }

    private void buildFundingParts(ProjectDocument projectDocument,
                                   ProjectDocumentDTO dto) {
        if (Objects.isNull(projectDocument.getFundingParts())) {
            projectDocument.setFundingParts(new HashSet<>());
        }

        dto.getFundingParts().forEach(partDTO -> {
            var part = buildFundingPart(partDTO, projectDocument);
            projectDocument.getFundingParts().add(part);
        });
    }

    private FundingPart buildFundingPart(FundingPartDTO dto, ProjectDocument parent) {
        var part = new FundingPart();

        part.setDescription(
                multilingualContentService.getMultilingualContent(dto.getDescription()));

        part.setAmount(new MonetaryAmount());
        part.getAmount().setCurrency(
                currencyService.findOne(dto.getAmount().getCurrencyId()));
        part.getAmount().setAmount(dto.getAmount().getAmount());

        if (Objects.nonNull(dto.getFundingId())) {
            part.setProjectDocument(parent);
        }

        return part;
    }


}
