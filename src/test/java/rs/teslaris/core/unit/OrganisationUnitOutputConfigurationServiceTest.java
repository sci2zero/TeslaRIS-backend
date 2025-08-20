package rs.teslaris.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.institution.OrganisationUnitOutputConfigurationDTO;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitOutputConfiguration;
import rs.teslaris.core.repository.institution.OrganisationUnitOutputConfigurationRepository;
import rs.teslaris.core.service.impl.institution.OrganisationUnitOutputConfigurationServiceImpl;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;

@SpringBootTest
class OrganisationUnitOutputConfigurationServiceTest {

    @Mock
    private OrganisationUnitOutputConfigurationRepository
        organisationUnitOutputConfigurationRepository;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private OrganisationUnitOutputConfigurationServiceImpl service;


    @Test
    void shouldReturnConfigurationWhenConfigurationExists() {
        //given
        var organisationUnitId = 1;
        var existingConfiguration = new OrganisationUnitOutputConfiguration();
        existingConfiguration.setShowOutputs(false);
        existingConfiguration.setShowBySpecifiedAffiliation(true);
        existingConfiguration.setShowByPublicationYearEmployments(false);
        existingConfiguration.setShowByCurrentEmployments(true);

        when(organisationUnitOutputConfigurationRepository.findConfigurationForOrganisationUnit(
            organisationUnitId))
            .thenReturn(Optional.of(existingConfiguration));

        //when
        var result = service.readOutputConfigurationForOrganisationUnit(organisationUnitId);

        //then
        assertThat(result.showOutputs()).isFalse();
        assertThat(result.showBySpecifiedAffiliation()).isTrue();
        assertThat(result.showByPublicationYearEmployments()).isFalse();
        assertThat(result.showByCurrentEmployments()).isTrue();
    }

    @Test
    void shouldReturnDefaultConfigurationWhenConfigurationDoesNotExist() {
        //given
        var organisationUnitId = 2;
        when(organisationUnitOutputConfigurationRepository.findConfigurationForOrganisationUnit(
            organisationUnitId))
            .thenReturn(Optional.empty());

        //when
        var result = service.readOutputConfigurationForOrganisationUnit(organisationUnitId);

        //then
        assertThat(result.showOutputs()).isTrue();
        assertThat(result.showBySpecifiedAffiliation()).isTrue();
        assertThat(result.showByPublicationYearEmployments()).isTrue();
        assertThat(result.showByCurrentEmployments()).isTrue();
    }

    @Test
    void shouldUpdateConfigurationWhenConfigurationExists() {
        //given
        var organisationUnitId = 3;
        var dto = new OrganisationUnitOutputConfigurationDTO(false, false, true, false);

        var existingConfiguration = new OrganisationUnitOutputConfiguration();
        existingConfiguration.setShowOutputs(true);
        existingConfiguration.setShowBySpecifiedAffiliation(true);
        existingConfiguration.setShowByPublicationYearEmployments(true);
        existingConfiguration.setShowByCurrentEmployments(true);

        when(organisationUnitOutputConfigurationRepository.findConfigurationForOrganisationUnit(
            organisationUnitId))
            .thenReturn(Optional.of(existingConfiguration));

        //when
        var result = service.saveConfiguration(dto, organisationUnitId);

        //then
        assertThat(result).isEqualTo(dto);
        assertThat(existingConfiguration.getShowOutputs()).isFalse();
        assertThat(existingConfiguration.getShowBySpecifiedAffiliation()).isFalse();
        assertThat(existingConfiguration.getShowByPublicationYearEmployments()).isTrue();
        assertThat(existingConfiguration.getShowByCurrentEmployments()).isFalse();
        verify(organisationUnitOutputConfigurationRepository).findConfigurationForOrganisationUnit(
            organisationUnitId);
    }

    @Test
    void shouldCreateNewConfigurationWhenConfigurationDoesNotExist() {
        //given
        var organisationUnitId = 4;
        var dto = new OrganisationUnitOutputConfigurationDTO(true, false, false, true);

        var organisationUnit = new OrganisationUnit();
        organisationUnit.setId(organisationUnitId);

        when(organisationUnitOutputConfigurationRepository.findConfigurationForOrganisationUnit(
            organisationUnitId))
            .thenReturn(Optional.empty());
        when(organisationUnitService.findOrganisationUnitById(organisationUnitId))
            .thenReturn(organisationUnit);

        //when
        var result = service.saveConfiguration(dto, organisationUnitId);

        //then
        assertThat(result).isEqualTo(dto);
        verify(organisationUnitOutputConfigurationRepository).findConfigurationForOrganisationUnit(
            organisationUnitId);
        verify(organisationUnitService).findOrganisationUnitById(organisationUnitId);
    }
}

