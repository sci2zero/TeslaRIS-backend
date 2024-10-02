package rs.teslaris.core.service.impl.assessment;

import java.util.HashSet;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.assessment.CommissionConverter;
import rs.teslaris.core.dto.assessment.CommissionDTO;
import rs.teslaris.core.model.assessment.Commission;
import rs.teslaris.core.repository.assessment.CommissionRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.assessment.CommissionService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.CommissionReferenceConstraintViolationException;

@Service
@RequiredArgsConstructor
@Transactional
public class CommissionServiceImpl extends JPAServiceImpl<Commission> implements CommissionService {

    private final CommissionRepository commissionRepository;

    private final OrganisationUnitService organisationUnitService;

    private final PersonService personService;

    private final DocumentPublicationService documentPublicationService;

    private final MultilingualContentService multilingualContentService;


    @Override
    protected JpaRepository<Commission, Integer> getEntityRepository() {
        return commissionRepository;
    }

    @Override
    public Page<CommissionDTO> readAllCommissions(Pageable pageable) {
        return commissionRepository.findAll(pageable).map(CommissionConverter::toDTO);
    }

    @Override
    public CommissionDTO readCommissionById(Integer commissionId) {
        return CommissionConverter.toDTO(findOne(commissionId));
    }

    @Override
    public Commission createCommission(CommissionDTO commissionDTO) {
        var newCommission = new Commission();

        setCommonFields(newCommission, commissionDTO);

        return save(newCommission);
    }

    @Override
    public void updateCommission(Integer commissionId, CommissionDTO commissionDTO) {
        var commissionToUpdate = findOne(commissionId);

        clearCommonFields(commissionToUpdate);
        setCommonFields(commissionToUpdate, commissionDTO);

        save(commissionToUpdate);
    }

    @Override
    public void deleteCommission(Integer commissionId) {
        if (commissionRepository.isInUse(commissionId)) {
            throw new CommissionReferenceConstraintViolationException("Commission already in use.");
        }

        delete(commissionId);
    }

    private void setCommonFields(Commission commission, CommissionDTO commissionDTO) {
        commission.setDescription(
            multilingualContentService.getMultilingualContent(commissionDTO.description()));
        commission.setSources(new HashSet<>(commissionDTO.sources()));
        commission.setAssessmentDateFrom(commissionDTO.assessmentDateFrom());
        commission.setAssessmentDateTo(commissionDTO.assessmentDateTo());
        commission.setFormalDescriptionOfRule(commissionDTO.formalDescriptionOfRule());

        if (Objects.nonNull(commissionDTO.superCommissionId())) {
            commission.setSuperComission(findOne(commissionDTO.superCommissionId()));
        }

        commissionDTO.documentIdsForAssessment().forEach(documentId -> {
            commission.getDocumentsForAssessment()
                .add(documentPublicationService.findOne(documentId));
        });

        commissionDTO.personIdsForAssessment().forEach(personId -> {
            commission.getPersonsForAssessment().add(personService.findOne(personId));
        });

        commissionDTO.organisationUnitIdsForAssessment().forEach(organisationUnitId -> {
            commission.getOrganisationUnitsForAssessment()
                .add(organisationUnitService.findOne(organisationUnitId));
        });
    }

    private void clearCommonFields(Commission commission) {
        commission.getDocumentsForAssessment().clear();
        commission.getPersonsForAssessment().clear();
        commission.getOrganisationUnitsForAssessment().clear();
    }
}
