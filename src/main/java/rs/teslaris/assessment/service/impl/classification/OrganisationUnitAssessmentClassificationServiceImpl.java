package rs.teslaris.assessment.service.impl.classification;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.repository.classification.EntityAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.classification.OrganisationUnitAssessmentClassificationRepository;
import rs.teslaris.assessment.service.interfaces.CommissionService;
import rs.teslaris.assessment.service.interfaces.classification.AssessmentClassificationService;
import rs.teslaris.assessment.service.interfaces.classification.OrganisationUnitAssessmentClassificationService;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;

@Service
@Traceable
public class OrganisationUnitAssessmentClassificationServiceImpl
    extends EntityAssessmentClassificationServiceImpl implements
    OrganisationUnitAssessmentClassificationService {

    private final OrganisationUnitAssessmentClassificationRepository
        organisationUnitAssessmentClassificationRepository;


    @Autowired
    public OrganisationUnitAssessmentClassificationServiceImpl(
        AssessmentClassificationService assessmentClassificationService,
        CommissionService commissionService, DocumentPublicationService documentPublicationService,
        ConferenceService conferenceService,
        ApplicationEventPublisher applicationEventPublisher,
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        OrganisationUnitAssessmentClassificationRepository organisationUnitAssessmentClassificationRepository) {
        super(assessmentClassificationService, commissionService, documentPublicationService,
            conferenceService, applicationEventPublisher, entityAssessmentClassificationRepository);
        this.organisationUnitAssessmentClassificationRepository =
            organisationUnitAssessmentClassificationRepository;
    }

    @Override
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForOrganisationUnit(
        Integer organisationUnitId) {
        return organisationUnitAssessmentClassificationRepository.findAssessmentClassificationsForOrganisationUnit(
                organisationUnitId).stream().map(EntityAssessmentClassificationConverter::toDTO)
            .sorted((a, b) -> b.year().compareTo(a.year()))
            .collect(Collectors.toList());
    }
}
