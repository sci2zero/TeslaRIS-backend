package rs.teslaris.core.assessment.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.model.AssessmentClassification;
import rs.teslaris.core.assessment.model.Commission;
import rs.teslaris.core.assessment.model.CommissionRelation;
import rs.teslaris.core.assessment.model.DocumentAssessmentClassification;
import rs.teslaris.core.assessment.repository.DocumentAssessmentClassificationRepository;
import rs.teslaris.core.assessment.repository.EntityAssessmentClassificationRepository;
import rs.teslaris.core.assessment.repository.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.core.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.core.assessment.service.interfaces.CommissionService;
import rs.teslaris.core.assessment.service.interfaces.DocumentAssessmentClassificationService;
import rs.teslaris.core.assessment.util.ClassificationPriorityMapping;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.person.OrganisationUnitsRelationRepository;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@Transactional
@Slf4j
public class DocumentAssessmentClassificationServiceImpl
    extends EntityAssessmentClassificationServiceImpl implements
    DocumentAssessmentClassificationService {

    private final DocumentAssessmentClassificationRepository
        documentAssessmentClassificationRepository;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final UserService userService;

    private final OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    private final PublicationSeriesAssessmentClassificationRepository
        publicationSeriesAssessmentClassificationRepository;

    private final DocumentRepository documentRepository;

    private final TaskManagerService taskManagerService;


    @Autowired
    public DocumentAssessmentClassificationServiceImpl(
        AssessmentClassificationService assessmentClassificationService,
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        CommissionService commissionService,
        DocumentAssessmentClassificationRepository documentAssessmentClassificationRepository,
        DocumentPublicationIndexRepository documentPublicationIndexRepository,
        UserService userService,
        OrganisationUnitsRelationRepository organisationUnitsRelationRepository,
        PublicationSeriesAssessmentClassificationRepository publicationSeriesAssessmentClassificationRepository,
        DocumentRepository documentRepository, TaskManagerService taskManagerService) {
        super(assessmentClassificationService, entityAssessmentClassificationRepository,
            commissionService);
        this.documentAssessmentClassificationRepository =
            documentAssessmentClassificationRepository;
        this.documentPublicationIndexRepository = documentPublicationIndexRepository;
        this.userService = userService;
        this.organisationUnitsRelationRepository = organisationUnitsRelationRepository;
        this.publicationSeriesAssessmentClassificationRepository =
            publicationSeriesAssessmentClassificationRepository;
        this.documentRepository = documentRepository;
        this.taskManagerService = taskManagerService;
    }

    @Override
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForDocument(
        Integer documentId) {
        return documentAssessmentClassificationRepository.findAssessmentClassificationsForDocument(
                documentId).stream().map(EntityAssessmentClassificationConverter::toDTO)
            .sorted((a, b) -> b.year().compareTo(a.year()))
            .collect(Collectors.toList());
    }

    @Override
    public void scheduleJournalPublicationClassification(LocalDateTime timeToRun,
                                                         Integer userId, LocalDate fromDate) {
        taskManagerService.scheduleTask(
            "Journal_Publication_Assessment-From-" + fromDate + "-" + UUID.randomUUID(), timeToRun,
            () -> classifyJournalPublications(fromDate), userId);
    }

    @Override
    public void classifyJournalPublication(Integer journalPublicationId) {
        var journalPublicationIndex =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                journalPublicationId);
        if (journalPublicationIndex.isEmpty()) {
            throw new NotFoundException(
                "Journal publication with ID " + journalPublicationId + " does not exist");
        }

        journalPublicationIndex.get().getOrganisationUnitIds().forEach(organisationUnitId -> {
            performJournalPublicationAssessmentForOrganisationUnit(organisationUnitId,
                journalPublicationIndex.get().getJournalId(),
                journalPublicationIndex.get().getYear(),
                journalPublicationIndex.get().getDatabaseId());
        });
    }

    private void classifyJournalPublications(LocalDate fromDate) {
        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<DocumentPublicationIndex> chunk =
                documentPublicationIndexRepository.findAllByLastEditedAfterAndType(
                    fromDate.toString(),
                    DocumentPublicationType.JOURNAL_PUBLICATION.name(),
                    PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach(journalPublicationIndex -> {
                journalPublicationIndex.getOrganisationUnitIds().forEach(organisationUnitId -> {
                    performJournalPublicationAssessmentForOrganisationUnit(organisationUnitId,
                        journalPublicationIndex.getJournalId(),
                        journalPublicationIndex.getYear(), journalPublicationIndex.getDatabaseId());
                });
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    private void performJournalPublicationAssessmentForOrganisationUnit(
        Integer organisationUnitId, Integer publicationSeriesId, Integer classificationYear,
        Integer documentId) {

        var commission = findCommissionInHierarchy(organisationUnitId);

        if (commission.isEmpty()) {
            log.info("No commission found for organisation unit {} or its hierarchy.",
                organisationUnitId);
            return;
        }

        var classification = publicationSeriesAssessmentClassificationRepository
            .findAssessmentClassificationsForPublicationSeriesAndCommissionAndYear(
                publicationSeriesId, commission.get().getId(), classificationYear);

        if (classification.isPresent()) {
            handleClassification(classification.get().getAssessmentClassification(),
                commission.get(), documentId, classificationYear);
        } else {
            handleRelationAssessments(commission.get(), publicationSeriesId, classificationYear,
                documentId);
        }
    }

    private Optional<Commission> findCommissionInHierarchy(Integer organisationUnitId) {
        Optional<Commission> commission;
        do {
            commission = userService.findCommissionForOrganisationUnitId(organisationUnitId);
            if (commission.isEmpty()) {
                var superOU = organisationUnitsRelationRepository.getSuperOU(organisationUnitId);
                if (superOU.isPresent()) {
                    organisationUnitId = superOU.get().getId();
                } else {
                    break;
                }
            }
        } while (commission.isEmpty());
        return commission;
    }

    private void handleClassification(AssessmentClassification classification,
                                      Commission commission,
                                      Integer documentId, Integer classificationYear) {
        var mappedCode = ClassificationPriorityMapping.getDocClassificationCodeBasedOnPubSeriesCode(
            classification.getCode());
        if (mappedCode.isEmpty()) {
            return;
        }

        var documentClassification = assessmentClassificationService
            .readAssessmentClassificationByCode(mappedCode.get());
        saveDocumentClassification(documentClassification, commission, documentId,
            classificationYear);
    }

    private void handleRelationAssessments(Commission commission, Integer publicationSeriesId,
                                           Integer classificationYear, Integer documentId) {
        var sortedRelations = commission.getRelations().stream()
            .sorted(Comparator.comparingInt(CommissionRelation::getPriority))
            .toList();

        for (var relation : sortedRelations) {
            var respectedClassification =
                respectRelationAssessment(relation, classificationYear, publicationSeriesId);
            if (respectedClassification.isPresent()) {
                var mappedCode =
                    ClassificationPriorityMapping.getDocClassificationCodeBasedOnPubSeriesCode(
                        respectedClassification.get().getCode());
                if (mappedCode.isPresent()) {
                    var documentClassification = assessmentClassificationService
                        .readAssessmentClassificationByCode(mappedCode.get());
                    saveDocumentClassification(documentClassification,
                        relation.getSourceCommission(),
                        documentId, classificationYear);
                    return;
                }
            }
        }
    }

    private Optional<AssessmentClassification> respectRelationAssessment(
        CommissionRelation commissionRelation, Integer year,
        Integer publicationSeriesId) {
        var classifications = new ArrayList<AssessmentClassification>();
        commissionRelation.getTargetCommissions().forEach(targetCommission -> {
            var classification =
                publicationSeriesAssessmentClassificationRepository
                    .findAssessmentClassificationsForPublicationSeriesAndCommissionAndYear(
                        publicationSeriesId, targetCommission.getId(), year);
            classification.ifPresent(journalClassification -> classifications.add(
                journalClassification.getAssessmentClassification()));
        });

        if (classifications.isEmpty()) {
            return Optional.empty();
        }

        return ClassificationPriorityMapping.getClassificationBasedOnCriteria(classifications,
            commissionRelation.getResultCalculationMethod());
    }

    private void saveDocumentClassification(AssessmentClassification assessmentClassification,
                                            Commission commission, Integer documentId,
                                            Integer classificationYear) {
        var existingClassification =
            documentAssessmentClassificationRepository.findClassificationForDocumentAndCategoryAndCommission(
                documentId, commission.getId());
        existingClassification.ifPresent(documentAssessmentClassificationRepository::delete);

        var documentClassification = new DocumentAssessmentClassification();
        documentClassification.setTimestamp(LocalDateTime.now());
        documentClassification.setCommission(commission);
        documentClassification.setAssessmentClassification(assessmentClassification);
        documentClassification.setClassificationYear(classificationYear);
        documentClassification.setDocument(documentRepository.getReferenceById(documentId));

        documentAssessmentClassificationRepository.save(documentClassification);
    }

}
