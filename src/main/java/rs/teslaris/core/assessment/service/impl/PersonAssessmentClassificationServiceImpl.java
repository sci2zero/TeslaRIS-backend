package rs.teslaris.core.assessment.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.repository.EntityAssessmentClassificationRepository;
import rs.teslaris.core.assessment.repository.PersonAssessmentClassificationRepository;
import rs.teslaris.core.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.core.assessment.service.interfaces.CommissionService;
import rs.teslaris.core.assessment.service.interfaces.PersonAssessmentClassificationService;

@Service
public class PersonAssessmentClassificationServiceImpl
    extends EntityAssessmentClassificationServiceImpl implements
    PersonAssessmentClassificationService {

    private final PersonAssessmentClassificationRepository personAssessmentClassificationRepository;


    @Autowired
    public PersonAssessmentClassificationServiceImpl(
        AssessmentClassificationService assessmentClassificationService,
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        CommissionService commissionService,
        PersonAssessmentClassificationRepository personAssessmentClassificationRepository) {
        super(assessmentClassificationService, entityAssessmentClassificationRepository,
            commissionService);
        this.personAssessmentClassificationRepository = personAssessmentClassificationRepository;
    }

    @Override
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPerson(
        Integer personId) {
        return personAssessmentClassificationRepository.findAssessmentClassificationsForPerson(
                personId).stream().map(EntityAssessmentClassificationConverter::toDTO)
            .sorted((a, b) -> b.year().compareTo(a.year()))
            .collect(Collectors.toList());
    }
}
