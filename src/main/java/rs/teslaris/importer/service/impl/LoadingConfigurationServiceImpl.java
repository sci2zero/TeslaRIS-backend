package rs.teslaris.importer.service.impl;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.importer.LoadingConfigurationRepository;
import rs.teslaris.importer.dto.LoadingConfigurationDTO;
import rs.teslaris.importer.model.configuration.LoadingConfiguration;
import rs.teslaris.importer.service.interfaces.LoadingConfigurationService;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
public class LoadingConfigurationServiceImpl extends JPAServiceImpl<LoadingConfiguration>
    implements LoadingConfigurationService {

    private final LoadingConfigurationRepository loadingConfigurationRepository;

    private final OrganisationUnitService organisationUnitService;

    private final PersonService personService;

    private final UserService userService;

    private final InvolvementService involvementService;


    @Override
    public void saveLoadingConfiguration(Integer institutionId,
                                         LoadingConfigurationDTO loadingConfigurationDTO) {
        var existingConfig = loadingConfigurationRepository
            .getLoadingConfigurationForInstitution(institutionId);

        var config = existingConfig.orElseGet(LoadingConfiguration::new);

        config.setSmartLoadingByDefault(loadingConfigurationDTO.getSmartLoadingByDefault());
        config.setLoadedEntitiesAreUnmanaged(
            loadingConfigurationDTO.getLoadedEntitiesAreUnmanaged());
        config.setPriorityLoading(loadingConfigurationDTO.getPriorityLoading());

        if (existingConfig.isEmpty()) {
            config.setInstitution(organisationUnitService.findOne(institutionId));
        }

        loadingConfigurationRepository.save(config);
    }

    @Override
    public LoadingConfigurationDTO getLoadingConfigurationForResearcherUser(Integer userId) {
        var personId = personService.getPersonIdForUserId(userId);
        var institutionIds =
            involvementService.getDirectEmploymentInstitutionIdsForPerson(personId);

        for (var institutionId : institutionIds) {
            var config = findConfigurationForInstitutionOrSuper(institutionId);
            if (config.isPresent()) {
                var c = config.get();
                return new LoadingConfigurationDTO(c.getSmartLoadingByDefault(),
                    c.getLoadedEntitiesAreUnmanaged(), c.getPriorityLoading());
            }
        }

        return new LoadingConfigurationDTO(true, true, false);
    }

    @Override
    public LoadingConfigurationDTO getLoadingConfigurationForEmployeeUser(Integer userId) {
        var institutionId = userService.getUserOrganisationUnitId(userId);

        return provideLoadingConfigurationForInstitution(institutionId);
    }

    @Override
    public LoadingConfigurationDTO getLoadingConfigurationForAdminUser(Integer institutionId) {
        return provideLoadingConfigurationForInstitution(institutionId);
    }

    private Optional<LoadingConfiguration> findConfigurationForInstitutionOrSuper(
        int institutionId) {
        var directConfig =
            loadingConfigurationRepository.getLoadingConfigurationForInstitution(institutionId);
        if (directConfig.isPresent()) {
            return directConfig;
        }

        for (var superOuId : organisationUnitService.getSuperOUsHierarchyRecursive(institutionId)) {
            var superConfig =
                loadingConfigurationRepository.getLoadingConfigurationForInstitution(superOuId);
            if (superConfig.isPresent()) {
                return superConfig;
            }
        }

        return Optional.empty();
    }

    private LoadingConfigurationDTO provideLoadingConfigurationForInstitution(
        Integer institutionId) {
        var config = findConfigurationForInstitutionOrSuper(institutionId);
        if (config.isPresent()) {
            var c = config.get();
            return new LoadingConfigurationDTO(c.getSmartLoadingByDefault(),
                c.getLoadedEntitiesAreUnmanaged(), c.getPriorityLoading());
        }

        return new LoadingConfigurationDTO(true, true, false);
    }

    @Override
    protected JpaRepository<LoadingConfiguration, Integer> getEntityRepository() {
        return loadingConfigurationRepository;
    }
}
