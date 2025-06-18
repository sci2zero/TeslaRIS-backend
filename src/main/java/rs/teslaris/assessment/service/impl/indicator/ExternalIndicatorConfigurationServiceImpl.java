package rs.teslaris.assessment.service.impl.indicator;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.dto.indicator.ExternalIndicatorConfigurationDTO;
import rs.teslaris.assessment.model.indicator.ExternalIndicatorConfiguration;
import rs.teslaris.assessment.repository.indicator.ExternalIndicatorConfigurationRepository;
import rs.teslaris.assessment.service.interfaces.indicator.ExternalIndicatorConfigurationService;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;

@Service
@RequiredArgsConstructor
@Transactional
public class ExternalIndicatorConfigurationServiceImpl
    extends JPAServiceImpl<ExternalIndicatorConfiguration> implements
    ExternalIndicatorConfigurationService {

    private final ExternalIndicatorConfigurationRepository externalIndicatorConfigurationRepository;

    private final OrganisationUnitService organisationUnitService;

    private final DocumentPublicationService documentPublicationService;


    @Override
    public ExternalIndicatorConfigurationDTO readConfigurationForInstitution(
        Integer institutionId) {
        var institutionIds = new HashSet<>(List.of(institutionId));
        institutionIds.addAll(organisationUnitService.getSuperOUsHierarchyRecursive(institutionId));

        for (var id : institutionIds) {
            var configuration =
                externalIndicatorConfigurationRepository.findByInstitutionId(id);

            if (configuration.isPresent()) {
                return new ExternalIndicatorConfigurationDTO(
                    configuration.get().getShowAltmetric(),
                    configuration.get().getShowDimensions(),
                    configuration.get().getShowOpenCitations(),
                    configuration.get().getShowPlumX(),
                    configuration.get().getShowUnpaywall());
            }
        }


        return new ExternalIndicatorConfigurationDTO(true, true, true, true, true);
    }

    @Override
    public ExternalIndicatorConfigurationDTO readConfigurationForDocument(Integer documentId) {
        var institutionIds = new HashSet<Integer>();
        documentPublicationService.findDocumentById(documentId).getContributors().stream()
            .filter(contribution -> Objects.nonNull(contribution.getPerson()))
            .forEach(contribution -> {
                contribution.getPerson().getInvolvements().stream()
                    .filter(involvement ->
                        Objects.nonNull(involvement.getOrganisationUnit()) &&
                            (involvement.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                                involvement.getInvolvementType().equals(InvolvementType.HIRED_BY))
                    ).forEach(involvement -> {
                        institutionIds.add(involvement.getOrganisationUnit().getId());
                    });
            });

        var configurations =
            institutionIds.stream().map(this::readConfigurationForInstitution).toList();

        return new ExternalIndicatorConfigurationDTO(
            configurations.stream().anyMatch(ExternalIndicatorConfigurationDTO::showAltmetric),
            configurations.stream().anyMatch(ExternalIndicatorConfigurationDTO::showDimensions),
            configurations.stream().anyMatch(ExternalIndicatorConfigurationDTO::showOpenCitations),
            configurations.stream().anyMatch(ExternalIndicatorConfigurationDTO::showPlumX),
            configurations.stream().anyMatch(ExternalIndicatorConfigurationDTO::showUnpaywall)
        );
    }

    @Override
    public void updateConfiguration(ExternalIndicatorConfigurationDTO configuration,
                                    Integer institutionId) {
        var savedConfiguration =
            externalIndicatorConfigurationRepository.findByInstitutionId(institutionId)
                .orElse(new ExternalIndicatorConfiguration());

        savedConfiguration.setInstitution(organisationUnitService.findOne(institutionId));

        savedConfiguration.setShowAltmetric(configuration.showAltmetric());
        savedConfiguration.setShowDimensions(configuration.showDimensions());
        savedConfiguration.setShowOpenCitations(configuration.showOpenCitations());
        savedConfiguration.setShowPlumX(configuration.showPlumX());
        savedConfiguration.setShowUnpaywall(configuration.showUnpaywall());

        save(savedConfiguration);
    }

    @Override
    protected JpaRepository<ExternalIndicatorConfiguration, Integer> getEntityRepository() {
        return externalIndicatorConfigurationRepository;
    }
}
