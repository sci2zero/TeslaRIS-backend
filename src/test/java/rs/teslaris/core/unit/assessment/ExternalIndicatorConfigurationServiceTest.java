package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.assessment.dto.indicator.ExternalIndicatorConfigurationDTO;
import rs.teslaris.assessment.model.indicator.ExternalIndicatorConfiguration;
import rs.teslaris.assessment.repository.indicator.ExternalIndicatorConfigurationRepository;
import rs.teslaris.assessment.service.impl.indicator.ExternalIndicatorConfigurationServiceImpl;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;

@SpringBootTest
public class ExternalIndicatorConfigurationServiceTest {

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private DocumentPublicationService documentPublicationService;

    @Mock
    private ExternalIndicatorConfigurationRepository externalIndicatorConfigurationRepository;

    @InjectMocks
    private ExternalIndicatorConfigurationServiceImpl service;


    private ExternalIndicatorConfigurationDTO fullTrueDTO() {
        return new ExternalIndicatorConfigurationDTO(true, true, true, true, true);
    }

    @Test
    void shouldReadConfigurationForInstitution() {
        var institutionId = 1;
        var superInstitutionId = 2;

        when(organisationUnitService.getSuperOUsHierarchyRecursive(institutionId))
            .thenReturn(List.of(superInstitutionId));

        var config = new ExternalIndicatorConfiguration();
        config.setShowAltmetric(false);
        config.setShowDimensions(true);
        config.setShowOpenCitations(false);
        config.setShowPlumX(true);
        config.setShowUnpaywall(false);

        when(externalIndicatorConfigurationRepository.findByInstitutionId(institutionId))
            .thenReturn(Optional.empty());
        when(externalIndicatorConfigurationRepository.findByInstitutionId(superInstitutionId))
            .thenReturn(Optional.of(config));

        var result = service.readConfigurationForInstitution(institutionId);

        assertEquals(new ExternalIndicatorConfigurationDTO(false, true, false, true, false),
            result);
    }

    @Test
    void shouldReturnDefaultIfNotFoundWhenReadingConfigurationForInstitution() {
        var institutionId = 1;
        when(organisationUnitService.getSuperOUsHierarchyRecursive(institutionId)).thenReturn(
            List.of());

        when(externalIndicatorConfigurationRepository.findByInstitutionId(institutionId))
            .thenReturn(Optional.empty());

        var result = service.readConfigurationForInstitution(institutionId);
        assertEquals(fullTrueDTO(), result);
    }

    @Test
    void shouldReadConfigurationForDocument() {
        var documentId = 1;
        var institutionId1 = 101;
        var institutionId2 = 202;

        var ou1 = new OrganisationUnit();
        ou1.setId(institutionId1);
        var ou2 = new OrganisationUnit();
        ou2.setId(institutionId2);

        var inv1 = new Employment();
        inv1.setOrganisationUnit(ou1);
        inv1.setInvolvementType(InvolvementType.EMPLOYED_AT);
        var inv2 = new Employment();
        inv2.setOrganisationUnit(ou2);
        inv2.setInvolvementType(InvolvementType.HIRED_BY);

        var person = new Person();
        person.setInvolvements(Set.of(inv1, inv2));

        var contribution = new PersonDocumentContribution();
        contribution.setPerson(person);

        var document = new Software();
        document.setContributors(Set.of(contribution));

        when(documentPublicationService.findDocumentById(documentId)).thenReturn(document);
        when(organisationUnitService.getSuperOUsHierarchyRecursive(any())).thenReturn(List.of());

        when(externalIndicatorConfigurationRepository.findByInstitutionId(institutionId1))
            .thenReturn(Optional.of(configWith(false, true, true, false, true)));
        when(externalIndicatorConfigurationRepository.findByInstitutionId(institutionId2))
            .thenReturn(Optional.of(configWith(true, false, false, false, false)));

        ExternalIndicatorConfigurationDTO result = service.readConfigurationForDocument(documentId);

        assertEquals(new ExternalIndicatorConfigurationDTO(true, true, true, false, true), result);
    }

    private ExternalIndicatorConfiguration configWith(boolean alt, boolean dim, boolean oc,
                                                      boolean plum, boolean up) {
        var config = new ExternalIndicatorConfiguration();
        config.setShowAltmetric(alt);
        config.setShowDimensions(dim);
        config.setShowOpenCitations(oc);
        config.setShowPlumX(plum);
        config.setShowUnpaywall(up);
        return config;
    }

    @Test
    void shouldUpdateConfiguration() {
        var institutionId = 5;
        var ou = new OrganisationUnit();
        ou.setId(institutionId);

        var existing = new ExternalIndicatorConfiguration();
        existing.setInstitution(ou);

        var dto =
            new ExternalIndicatorConfigurationDTO(true, false, true, false, true);

        when(externalIndicatorConfigurationRepository.findByInstitutionId(institutionId))
            .thenReturn(Optional.of(existing));
        when(organisationUnitService.findOne(institutionId)).thenReturn(ou);

        service.updateConfiguration(dto, institutionId);

        assertEquals(true, existing.getShowAltmetric());
        assertEquals(false, existing.getShowDimensions());
        assertEquals(true, existing.getShowOpenCitations());
        assertEquals(false, existing.getShowPlumX());
        assertEquals(true, existing.getShowUnpaywall());
    }
}
