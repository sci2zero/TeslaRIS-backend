package rs.teslaris.core.assessment.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.converter.CommissionConverter;
import rs.teslaris.core.assessment.dto.CommissionDTO;
import rs.teslaris.core.assessment.dto.CommissionResponseDTO;
import rs.teslaris.core.assessment.model.Commission;
import rs.teslaris.core.assessment.repository.CommissionRepository;
import rs.teslaris.core.assessment.ruleengine.JournalClassificationRuleEngine;
import rs.teslaris.core.assessment.service.interfaces.CommissionService;
import rs.teslaris.core.service.impl.JPAServiceImpl;
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
    public Page<CommissionResponseDTO> readAllCommissions(Pageable pageable,
                                                          String searchExpression,
                                                          String language) {
        if (Objects.nonNull(searchExpression)) {
            return commissionRepository.searchCommissions(searchExpression, language.toUpperCase(),
                    pageable)
                .map(CommissionConverter::toDTO);
        } else {
            return commissionRepository.findAll(pageable).map(CommissionConverter::toDTO);
        }
    }

    @Override
    public CommissionResponseDTO readCommissionById(Integer commissionId) {
        return CommissionConverter.toDTO(findOne(commissionId));
    }

    @Override
    public Commission createCommission(CommissionDTO commissionDTO) {
        var newCommission = new Commission();

        setCommonFields(newCommission, commissionDTO);

        return save(newCommission);
    }

    @Override
    public List<String> readAllApplicableRuleEngines() {
        var classNames = new ArrayList<String>();
        var provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AssignableTypeFilter(JournalClassificationRuleEngine.class));

        var components = provider.findCandidateComponents("rs/teslaris/core/assessment/ruleengine");
        for (var component : components) {
            try {
                var clazz = Class.forName(component.getBeanClassName());
                classNames.add(clazz.getName().split("\\.")[5]);
            } catch (ClassNotFoundException e) {
                // not going to happen as this class exists by default
            }
        }
        return classNames;
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
            multilingualContentService.getMultilingualContentAndSetDefaultsIfNonExistent(
                commissionDTO.description()));
        commission.setSources(new HashSet<>(commissionDTO.sources()));
        commission.setAssessmentDateFrom(commissionDTO.assessmentDateFrom());
        commission.setAssessmentDateTo(commissionDTO.assessmentDateTo());
        commission.setFormalDescriptionOfRule(commissionDTO.formalDescriptionOfRule());

        if (Objects.nonNull(commissionDTO.superCommissionId())) {
            commission.setSuperCommission(findOne(commissionDTO.superCommissionId()));
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
