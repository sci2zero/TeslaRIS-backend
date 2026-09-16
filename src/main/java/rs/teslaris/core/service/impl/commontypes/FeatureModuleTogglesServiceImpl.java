package rs.teslaris.core.service.impl.commontypes;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.commontypes.FeatureModuleTogglesDTO;
import rs.teslaris.core.model.commontypes.FeatureModuleToggles;
import rs.teslaris.core.repository.commontypes.FeatureModuleTogglesRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.FeatureModuleTogglesService;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureModuleTogglesServiceImpl extends JPAServiceImpl<FeatureModuleToggles>
    implements FeatureModuleTogglesService {

    private final FeatureModuleTogglesRepository featureModuleTogglesRepository;


    @Override
    protected JpaRepository<FeatureModuleToggles, Integer> getEntityRepository() {
        return featureModuleTogglesRepository;
    }

    @Override
    @Transactional
    public FeatureModuleTogglesDTO readConfigurationForSystem() {
        return findConfiguration().map(
                featureModuleToggles -> new FeatureModuleTogglesDTO(
                    featureModuleToggles.getToggleAssessmentModule(),
                    featureModuleToggles.getToggleDigitalLibrary(),
                    featureModuleToggles.getToggleDigitalRepository()))
            .orElseGet(this::readDefaultConfiguration);
    }

    @Override
    @Transactional
    public FeatureModuleTogglesDTO saveConfiguration(FeatureModuleTogglesDTO dto) {
        var configuration = findConfiguration().orElseGet(FeatureModuleToggles::new);

        configuration.setToggleAssessmentModule(dto.toggleAssessmentModule());
        configuration.setToggleDigitalLibrary(dto.toggleDigitalLibrary());
        configuration.setToggleDigitalRepository(dto.toggleDigitalRepository());
        save(configuration);

        return dto;
    }

    private Optional<FeatureModuleToggles> findConfiguration() {
        var configurations = featureModuleTogglesRepository.findAll(Sort.by("id"));

        if (configurations.isEmpty()) {
            return Optional.empty();
        }

        if (configurations.size() > 1) {
            var redundantConfigurations = configurations.subList(1, configurations.size());
            log.warn("Found {} feature module toggle configurations, keeping the oldest (id={}) " +
                    "and deleting the rest.", configurations.size(),
                configurations.getFirst().getId());
            featureModuleTogglesRepository.deleteAll(redundantConfigurations);
        }

        return Optional.of(configurations.getFirst());
    }

    private FeatureModuleTogglesDTO readDefaultConfiguration() {
        return new FeatureModuleTogglesDTO(true, true, true);
    }
}
