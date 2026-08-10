package rs.teslaris.project.service.impl.project;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.util.exceptionhandling.exception.MissingDataException;
import rs.teslaris.project.converter.project.ProjectDocumentConverter;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.dto.project.ProjectDocumentDTO;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.funding.FundingPart;
import rs.teslaris.project.model.project.ProjectDocument;
import rs.teslaris.project.repository.project.ProjectDocumentRepository;
import rs.teslaris.project.service.interfaces.project.ProjectDocumentService;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@Service
@RequiredArgsConstructor
public class ProjectDocumentServiceImpl extends JPAServiceImpl<ProjectDocument>
    implements ProjectDocumentService {

    private final ProjectDocumentRepository projectDocumentRepository;

    private final MultilingualContentService multilingualContentService;

    private final ProjectService projectService;

    private final DocumentPublicationService documentService;

    private final IndexBulkUpdateService indexBulkUpdateService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final CurrencyService currencyService;

    @Override
    protected JpaRepository<ProjectDocument, Integer> getEntityRepository() {
        return projectDocumentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDocumentDTO> readProjectDocuments(Integer projectId) {
        var projectDocuments = projectDocumentRepository.findAllByProjectId(projectId).stream()
            .map(ProjectDocumentConverter::toDTO)
            .toList();

        setDisplayFieldsFromIndex(projectDocuments);

        return projectDocuments;
    }

    private void setDisplayFieldsFromIndex(List<ProjectDocumentDTO> projectDocuments) {
        var documentIds = projectDocuments.stream()
            .map(ProjectDocumentDTO::getDocumentId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        if (documentIds.isEmpty()) {
            return;
        }

        var indexes = documentPublicationIndexRepository
            .findDocumentPublicationIndexByDatabaseIdIn(documentIds,
                PageRequest.of(0, documentIds.size()))
            .getContent().stream()
            .collect(Collectors.toMap(DocumentPublicationIndex::getDatabaseId, index -> index,
                (first, second) -> first));

        projectDocuments.forEach(projectDocument -> {
            var index = indexes.get(projectDocument.getDocumentId());
            if (Objects.nonNull(index)) {
                projectDocument.setDocumentTitleSr(index.getTitleSr());
                projectDocument.setDocumentTitleOther(index.getTitleOther());
                projectDocument.setDocumentType(index.getType());
            }
        });
    }

    @Override
    @Transactional
    public ProjectDocument createProjectDocument(ProjectDocumentDTO projectDocumentDTO) {
        var newProjectDocument = new ProjectDocument();

        setCommonFields(newProjectDocument, projectDocumentDTO);

        var savedProjectDocument = save(newProjectDocument);

        if (Objects.nonNull(savedProjectDocument.getDocument())) {
            indexBulkUpdateService.setIdFieldForRecord("document_publication", "databaseId",
                savedProjectDocument.getDocument().getId(), "project_id",
                savedProjectDocument.getProject().getId());
        }

        return savedProjectDocument;
    }

    @Override
    @Transactional
    public void deleteProjectDocument(Integer projectDocumentId) {
        var projectDocument = findOne(projectDocumentId);

        var document = projectDocument.getDocument();

        delete(projectDocumentId);

        if (Objects.nonNull(document)) {
            indexBulkUpdateService.setIdFieldForRecord("document_publication", "databaseId",
                document.getId(), "project_id", null);
        }
    }

    private void setCommonFields(ProjectDocument projectDocument, ProjectDocumentDTO dto) {
        if (Objects.isNull(dto.getDocumentId()) &&
            (Objects.isNull(dto.getTextualDescription()) ||
                dto.getTextualDescription().isEmpty())) {
            throw new MissingDataException(
                "Either a document or a textual description has to be provided.");
        }

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
