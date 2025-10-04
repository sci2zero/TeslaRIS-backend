package rs.teslaris.core.service.impl.institution;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.institution.OrganisationUnitOutputConfigurationDTO;
import rs.teslaris.core.model.institution.OrganisationUnitOutputConfiguration;
import rs.teslaris.core.repository.institution.OrganisationUnitOutputConfigurationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitOutputConfigurationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganisationUnitOutputConfigurationServiceImpl
    extends JPAServiceImpl<OrganisationUnitOutputConfiguration>
    implements OrganisationUnitOutputConfigurationService {

    private final OrganisationUnitOutputConfigurationRepository
        organisationUnitOutputConfigurationRepository;

    private final OrganisationUnitService organisationUnitService;


    @Override
    protected JpaRepository<OrganisationUnitOutputConfiguration, Integer> getEntityRepository() {
        return organisationUnitOutputConfigurationRepository;
    }

    @Override
    public OrganisationUnitOutputConfigurationDTO readOutputConfigurationForOrganisationUnit(
        Integer organisationUnitId) {
        var configuration =
            organisationUnitOutputConfigurationRepository.findConfigurationForOrganisationUnit(
                organisationUnitId);

        return configuration.map(outputConfiguration -> new OrganisationUnitOutputConfigurationDTO(
                outputConfiguration.getShowOutputs(),
                outputConfiguration.getShowBySpecifiedAffiliation(),
                outputConfiguration.getShowByPublicationYearEmployments(),
                outputConfiguration.getShowByCurrentEmployments()))
            .orElseGet(() -> new OrganisationUnitOutputConfigurationDTO(true, true, true, true));
    }

    @Override
    public OrganisationUnitOutputConfigurationDTO saveConfiguration(
        OrganisationUnitOutputConfigurationDTO dto, Integer organisationUnitId) {
        var configuration =
            organisationUnitOutputConfigurationRepository.findConfigurationForOrganisationUnit(
                organisationUnitId).orElseGet(() -> {
                var config = new OrganisationUnitOutputConfiguration();
                config.setOrganisationUnit(
                    organisationUnitService.findOrganisationUnitById(organisationUnitId));
                return config;
            });

        configuration.setShowOutputs(dto.showOutputs());
        configuration.setShowBySpecifiedAffiliation(dto.showBySpecifiedAffiliation());
        configuration.setShowByPublicationYearEmployments(dto.showByPublicationYearEmployments());
        configuration.setShowByCurrentEmployments(dto.showByCurrentEmployments());
        save(configuration);

        return dto;
    }
}
