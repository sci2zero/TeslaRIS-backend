package rs.teslaris.core.assessment.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import rs.teslaris.core.assessment.util.ClassificationMappingConfigurationLoader;
import rs.teslaris.core.assessment.util.ResearchAreasConfigurationLoader;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.CommissionReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

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
                                                          String language,
                                                          Boolean selectOnlyLoadCommissions,
                                                          Boolean selectOnlyClassificationCommissions) {
        if (Objects.nonNull(searchExpression)) {
            return filterAndPage(
                commissionRepository.searchCommissions(searchExpression, language.toUpperCase(),
                    pageable),
                selectOnlyLoadCommissions,
                selectOnlyClassificationCommissions,
                pageable
            );
        } else {
            return filterAndPage(
                commissionRepository.readAll(language.toUpperCase(), pageable),
                selectOnlyLoadCommissions,
                selectOnlyClassificationCommissions,
                pageable
            );
        }
    }

    private Page<CommissionResponseDTO> filterAndPage(Page<Commission> commissions,
                                                      Boolean selectOnlyLoadCommissions,
                                                      Boolean selectOnlyClassificationCommissions,
                                                      Pageable pageable) {
        List<CommissionResponseDTO> filteredList = commissions.getContent().stream()
            .filter(commission -> {
                if (selectOnlyLoadCommissions) {
                    return commission.getFormalDescriptionOfRule().startsWith("load-");
                } else if (selectOnlyClassificationCommissions) {
                    return !commission.getFormalDescriptionOfRule().startsWith("load-");
                }
                return true;
            })
            .map(CommissionConverter::toDTO)
            .toList();

        return new PageImpl<>(filteredList, pageable, commissions.getTotalElements());
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

        ClassificationMappingConfigurationLoader.fetchAllConfigurationNames()
            .forEach(configurationName -> {
                classNames.add("load-" + configurationName);
            });

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

    @Override
    public Commission findOneWithFetchedRelations(Integer commissionId) {
        return commissionRepository.findOneWithRelations(commissionId).orElseThrow(
            () -> new NotFoundException("Commission with ID " + commissionId + " does not exist."));
    }

    private void setCommonFields(Commission commission, CommissionDTO commissionDTO) {
        commission.setDescription(
            multilingualContentService.getMultilingualContentAndSetDefaultsIfNonExistent(
                commissionDTO.description()));
//        commission.setSources(new HashSet<>(commissionDTO.sources()));
        commission.setAssessmentDateFrom(commissionDTO.assessmentDateFrom());
        commission.setAssessmentDateTo(commissionDTO.assessmentDateTo());

        if (!readAllApplicableRuleEngines().contains(commissionDTO.formalDescriptionOfRule())) {
            throw new NotFoundException("Specified rule engine is not available.");
        }
        commission.setFormalDescriptionOfRule(commissionDTO.formalDescriptionOfRule());

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

        commissionDTO.recognisedResearchAreas().forEach(researchAreaCode -> {
            if (ResearchAreasConfigurationLoader.codeExists(researchAreaCode)) {
                commission.getRecognisedResearchAreas().add(researchAreaCode);
            }
        });
    }

    private void clearCommonFields(Commission commission) {
        commission.getDocumentsForAssessment().clear();
        commission.getPersonsForAssessment().clear();
        commission.getOrganisationUnitsForAssessment().clear();
        commission.getRecognisedResearchAreas().clear();
    }
}
