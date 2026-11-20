package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.repository.MigrationRecordLogRepository;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationException;
import rs.teslaris.migrator.util.ResolvedToExistingException;
import rs.teslaris.project.dto.project.ProjectDocumentDTO;
import rs.teslaris.project.model.project.ProjectDocumentType;
import rs.teslaris.project.repository.project.ProjectDocumentRepository;
import rs.teslaris.project.service.interfaces.funding.FundingService;
import rs.teslaris.project.service.interfaces.project.ProjectDocumentService;

/**
 * Links the project of a curriculum funding with the document of a curriculum output as a RESULT.
 * A side that was not migrated fails the item, so a later run links it once both exist.
 */
@Component
@RequiredArgsConstructor
public class ProjectDocumentEntityCreator implements EntityCreator<ProjectDocumentMigrationDTO> {

    // The output's own document; a presented paper's slides share its key and are not the result
    private static final List<MigrationEntityType> OUTPUT_TYPES = List.of(
        MigrationEntityType.JOURNAL_PUBLICATION,
        MigrationEntityType.PROCEEDINGS_PUBLICATION,
        MigrationEntityType.CONFERENCE_ABSTRACT,
        MigrationEntityType.CONFERENCE_POSTER,
        MigrationEntityType.MONOGRAPH_PUBLICATION,
        MigrationEntityType.MONOGRAPH,
        MigrationEntityType.THESIS
    );

    private final MigrationRecordLogRepository recordLogRepository;

    private final FundingService fundingService;

    private final ProjectDocumentService projectDocumentService;

    private final ProjectDocumentRepository projectDocumentRepository;


    @Override
    public Integer create(ProjectDocumentMigrationDTO dto, MigrationRun run) {
        dto.throwIfRejected();

        var documentId = documentId(dto.outputKey()).orElseThrow(() ->
            new MigrationException("output " + dto.outputKey() + " has no migrated document"));
        var projectId = projectId(dto.fundingKey());

        projectDocumentRepository.findAllByProjectId(projectId).stream()
            .filter(link -> Objects.nonNull(link.getDocument()) &&
                documentId.equals(link.getDocument().getId()))
            .findFirst()
            .ifPresent(link -> {
                throw new ResolvedToExistingException(link.getId(),
                    "document " + documentId + " already linked to project " + projectId);
            });

        var link = new ProjectDocumentDTO();
        link.setProjectId(projectId);
        link.setDocumentId(documentId);
        link.setRelationType(ProjectDocumentType.RESULT);
        link.setTextualDescription(new ArrayList<>());

        return projectDocumentService.createProjectDocument(link).getId();
    }

    private Optional<Integer> documentId(String outputKey) {
        return OUTPUT_TYPES.stream()
            .map(type -> recordLogRepository.findTargetEntityId(HydratorSource.NAME, type,
                outputKey))
            .flatMap(Optional::stream)
            .findFirst();
    }

    // The record log keeps the funding; a funding matched to an existing project records no id
    private Integer projectId(String fundingKey) {
        var fundingId = recordLogRepository.findTargetEntityId(HydratorSource.NAME,
            MigrationEntityType.CV_FUNDING, fundingKey).orElseThrow(() ->
            new MigrationException("funding " + fundingKey + " has no migrated funding"));
        var projectId = fundingService.readFunding(fundingId).getProjectId();

        if (Objects.isNull(projectId)) {
            throw new MigrationException("funding " + fundingId + " has no project");
        }

        return projectId;
    }
}
