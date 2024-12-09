package rs.teslaris.core.assessment.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.dto.PublicationSeriesAssessmentClassificationDTO;
import rs.teslaris.core.assessment.model.PublicationSeriesAssessmentClassification;
import rs.teslaris.core.assessment.repository.EntityAssessmentClassificationRepository;
import rs.teslaris.core.assessment.repository.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.core.assessment.service.impl.cruddelegate.PublicationSeriesAssessmentClassificationJPAServiceImpl;
import rs.teslaris.core.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.core.assessment.service.interfaces.CommissionService;
import rs.teslaris.core.assessment.service.interfaces.PublicationSeriesAssessmentClassificationService;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesService;

@Service
public class PublicationSeriesAssessmentClassificationServiceImpl
    extends EntityAssessmentClassificationServiceImpl
    implements PublicationSeriesAssessmentClassificationService {

    private final PublicationSeriesAssessmentClassificationJPAServiceImpl
        publicationSeriesAssessmentClassificationJPAService;

    private final PublicationSeriesAssessmentClassificationRepository
        publicationSeriesAssessmentClassificationRepository;

    private final PublicationSeriesService publicationSeriesService;

    @Autowired
    public PublicationSeriesAssessmentClassificationServiceImpl(
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        CommissionService commissionService,
        AssessmentClassificationService assessmentClassificationService,
        PublicationSeriesAssessmentClassificationJPAServiceImpl publicationSeriesAssessmentClassificationJPAService,
        PublicationSeriesAssessmentClassificationRepository publicationSeriesAssessmentClassificationRepository,
        PublicationSeriesService publicationSeriesService) {
        super(entityAssessmentClassificationRepository, commissionService,
            assessmentClassificationService);
        this.publicationSeriesAssessmentClassificationJPAService =
            publicationSeriesAssessmentClassificationJPAService;
        this.publicationSeriesAssessmentClassificationRepository =
            publicationSeriesAssessmentClassificationRepository;
        this.publicationSeriesService = publicationSeriesService;
    }


    @Override
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPublicationSeries(
        Integer publicationSeriesId) {
        return publicationSeriesAssessmentClassificationRepository.findAssessmentClassificationsForPublicationSeries(
                publicationSeriesId).stream().map(EntityAssessmentClassificationConverter::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public PublicationSeriesAssessmentClassification createPublicationSeriesAssessmentClassification(
        PublicationSeriesAssessmentClassificationDTO publicationSeriesAssessmentClassificationDTO) {
        var newAssessmentClassification = new PublicationSeriesAssessmentClassification();

        setCommonFields(newAssessmentClassification, publicationSeriesAssessmentClassificationDTO);
        newAssessmentClassification.setPublicationSeries(
            publicationSeriesService.findOne(
                publicationSeriesAssessmentClassificationDTO.getPublicationSeriesId()));

        return publicationSeriesAssessmentClassificationJPAService.save(
            newAssessmentClassification);
    }

    @Override
    public void updatePublicationSeriesAssessmentClassification(
        Integer publicationSeriesAssessmentClassificationId,
        PublicationSeriesAssessmentClassificationDTO publicationSeriesAssessmentClassificationDTO) {
        var publicationSeriesAssessmentClassificationToUpdate =
            publicationSeriesAssessmentClassificationJPAService.findOne(
                publicationSeriesAssessmentClassificationId);

        setCommonFields(publicationSeriesAssessmentClassificationToUpdate,
            publicationSeriesAssessmentClassificationDTO);
        publicationSeriesAssessmentClassificationToUpdate.setPublicationSeries(
            publicationSeriesService.findOne(
                publicationSeriesAssessmentClassificationDTO.getPublicationSeriesId()));

        publicationSeriesAssessmentClassificationJPAService.save(
            publicationSeriesAssessmentClassificationToUpdate);
    }
}
