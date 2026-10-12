package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.commontypes.CrisContextInformationDTO;
import rs.teslaris.core.dto.commontypes.IdentifierConfigurationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.commontypes.CrisContextInformation;
import rs.teslaris.core.model.commontypes.IdentifierConfiguration;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.repository.commontypes.CrisContextInformationRepository;
import rs.teslaris.core.service.impl.commontypes.CrisContextInformationServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.persistence.IdentifierConfigurationCatalogue;

@SpringBootTest
public class CrisContextInformationServiceTest {

    @Mock
    private CrisContextInformationRepository crisContextInformationRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @InjectMocks
    private CrisContextInformationServiceImpl service;


    private CrisContextInformation configuration(Integer id, boolean assessment,
                                                 boolean library, boolean repository) {
        var configuration = new CrisContextInformation(assessment, library, repository, false,
            ".*", ".*", ".*", ".*", License.CC0, new HashMap<>(), new HashSet<>());
        configuration.setId(id);
        return configuration;
    }

    @Test
    public void shouldReportOrcidLoginEnabledWhenBothCredentialsAreSet() {
        // Given
        ReflectionTestUtils.setField(service, "orcidClientId", "APP-123");
        ReflectionTestUtils.setField(service, "orcidClientSecret", "secret");
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(configuration(1, true, true, true)));

        // When
        var result = service.readConfigurationForSystem();

        // Then
        assertTrue(result.orcidLoginToggle());
    }

    @Test
    public void shouldReportOrcidLoginDisabledWhenSecretIsMissing() {
        // Given
        ReflectionTestUtils.setField(service, "orcidClientId", "APP-123");
        ReflectionTestUtils.setField(service, "orcidClientSecret", "  ");
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(configuration(1, true, true, true)));

        // When
        var result = service.readConfigurationForSystem();

        // Then
        assertFalse(result.orcidLoginToggle());
    }

    @Test
    public void shouldReportOrcidLoginDisabledForUnconfiguredSentinel() {
        // Given
        ReflectionTestUtils.setField(service, "orcidClientId", "NOT_CONFIGURED");
        ReflectionTestUtils.setField(service, "orcidClientSecret", "NOT_CONFIGURED");
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(configuration(1, true, true, true)));

        // When
        var result = service.readConfigurationForSystem();

        // Then
        assertFalse(result.orcidLoginToggle());
    }

    @Test
    public void shouldIgnoreOrcidLoginToggleSentByClient() {
        // Given
        ReflectionTestUtils.setField(service, "orcidClientId", "");
        ReflectionTestUtils.setField(service, "orcidClientSecret", "");
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(Collections.emptyList());
        when(crisContextInformationRepository.save(any(CrisContextInformation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var dto = new CrisContextInformationDTO(true, true, true, true,
            ".*", ".*", ".*", ".*", License.CC0, defaultIdentifierConfiguration(),
            Collections.emptyList(), true);

        // When
        var result = service.saveConfiguration(dto);

        // Then the derived toggle wins over whatever the client sent
        assertFalse(result.orcidLoginToggle());
    }

    @Test
    public void shouldReturnExistingConfiguration() {
        // Given
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(configuration(1, false, true, false)));

        // When
        var result = service.readConfigurationForSystem();

        // Then
        assertFalse(result.toggleAssessmentModule());
        assertTrue(result.toggleDigitalLibrary());
        assertFalse(result.toggleDigitalRepository());
        verify(crisContextInformationRepository, never()).deleteAll(anyList());
    }

    @Test
    public void shouldReturnDefaultConfigurationWhenNoneExists() {
        // Given
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(Collections.emptyList());

        // When
        var result = service.readConfigurationForSystem();

        // Then
        assertTrue(result.toggleAssessmentModule());
        assertTrue(result.toggleDigitalLibrary());
        assertTrue(result.toggleDigitalRepository());
        verify(crisContextInformationRepository, never()).deleteAll(anyList());
    }

    @Test
    public void shouldKeepOldestAndDeleteRedundantConfigurationsOnRead() {
        // Given
        var oldest = configuration(1, false, false, true);
        var second = configuration(2, true, true, true);
        var third = configuration(3, true, false, false);
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(oldest, second, third));

        // When
        var result = service.readConfigurationForSystem();

        // Then
        assertFalse(result.toggleAssessmentModule());
        assertFalse(result.toggleDigitalLibrary());
        assertTrue(result.toggleDigitalRepository());

        var deletedCaptor = ArgumentCaptor.forClass(List.class);
        verify(crisContextInformationRepository).deleteAll(deletedCaptor.capture());
        assertEquals(List.of(second, third), deletedCaptor.getValue());
    }

    @Test
    public void shouldUpdateExistingConfigurationOnSave() {
        // Given
        var existing = configuration(1, true, true, true);
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(existing));
        when(crisContextInformationRepository.save(any(CrisContextInformation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var dto = new CrisContextInformationDTO(false, true, false, false,
            ".*", ".*", ".*", ".*", License.CC0, defaultIdentifierConfiguration(),
            Collections.emptyList(), false);

        // When
        var result = service.saveConfiguration(dto);

        // Then
        var savedCaptor = ArgumentCaptor.forClass(CrisContextInformation.class);
        verify(crisContextInformationRepository).save(savedCaptor.capture());
        assertSame(existing, savedCaptor.getValue());
        assertFalse(existing.getToggleAssessmentModule());
        assertTrue(existing.getToggleDigitalLibrary());
        assertFalse(existing.getToggleDigitalRepository());
        assertEquals(dto, result);
        verify(crisContextInformationRepository, never()).deleteAll(anyList());
    }

    @Test
    public void shouldCreateConfigurationOnSaveWhenNoneExists() {
        // Given
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(Collections.emptyList());
        when(crisContextInformationRepository.save(any(CrisContextInformation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var dto = new CrisContextInformationDTO(true, false, true, true,
            ".*", ".*", ".*", ".*", License.CC0, defaultIdentifierConfiguration(),
            Collections.emptyList(), false);

        // When
        var result = service.saveConfiguration(dto);

        // Then
        var savedCaptor = ArgumentCaptor.forClass(CrisContextInformation.class);
        verify(crisContextInformationRepository).save(savedCaptor.capture());
        var saved = savedCaptor.getValue();
        assertTrue(saved.getToggleAssessmentModule());
        assertFalse(saved.getToggleDigitalLibrary());
        assertTrue(saved.getToggleDigitalRepository());
        assertTrue(saved.getToggleRegistration());
        assertEquals(dto, result);
    }

    @Test
    public void shouldDeleteRedundantConfigurationsAndUpdateOldestOnSave() {
        // Given
        var oldest = configuration(1, true, true, true);
        var redundant = configuration(2, false, false, false);
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(oldest, redundant));
        when(crisContextInformationRepository.save(any(CrisContextInformation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var dto = new CrisContextInformationDTO(false, false, true, false,
            ".*", ".*", ".*", ".*", License.CC0, defaultIdentifierConfiguration(),
            Collections.emptyList(), false);

        // When
        service.saveConfiguration(dto);

        // Then
        var deletedCaptor = ArgumentCaptor.forClass(List.class);
        verify(crisContextInformationRepository).deleteAll(deletedCaptor.capture());
        assertEquals(List.of(redundant), deletedCaptor.getValue());

        var savedCaptor = ArgumentCaptor.forClass(CrisContextInformation.class);
        verify(crisContextInformationRepository).save(savedCaptor.capture());
        assertSame(oldest, savedCaptor.getValue());
        assertFalse(oldest.getToggleAssessmentModule());
        assertFalse(oldest.getToggleDigitalLibrary());
        assertTrue(oldest.getToggleDigitalRepository());
    }

    private Map<ApplicableEntityType, Map<String, IdentifierConfigurationDTO>>
    defaultIdentifierConfiguration() {
        var defaults =
            new LinkedHashMap<ApplicableEntityType, Map<String, IdentifierConfigurationDTO>>();

        IdentifierConfigurationCatalogue.configurableIdentifiers()
            .forEach((entityType, identifiers) -> {
                var perIdentifier = new LinkedHashMap<String, IdentifierConfigurationDTO>();
                identifiers.forEach(identifier -> perIdentifier.put(identifier,
                    new IdentifierConfigurationDTO(true, false, true)));
                defaults.put(entityType, perIdentifier);
            });

        return defaults;
    }

    private CrisContextInformationDTO dtoWithIdentifierConfiguration(
        Map<ApplicableEntityType, Map<String, IdentifierConfigurationDTO>> configuration) {
        return new CrisContextInformationDTO(true, true, true, true,
            ".*", ".*", ".*", ".*", License.CC0, configuration,
            Collections.emptyList(), false);
    }

    private CrisContextInformation configurationWithStoredPolicy(ApplicableEntityType entityType,
                                                                 String identifier,
                                                                 IdentifierConfiguration policy) {
        var configuration = configuration(1, true, true, true);
        configuration.setIdentifierConfiguration(
            new HashMap<>(Map.of(entityType, new HashMap<>(Map.of(identifier, policy)))));
        return configuration;
    }

    @Test
    public void shouldAnswerWithEveryConfigurableIdentifierWhenNoneIsConfigured() {
        // Given
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(Collections.emptyList());

        // When
        var result = service.readConfigurationForSystem();

        // Then
        assertEquals(IdentifierConfigurationCatalogue.configurableIdentifiers().size(),
            result.identifierConfiguration().size());

        IdentifierConfigurationCatalogue.configurableIdentifiers()
            .forEach((entityType, identifiers) -> {
                var answered = result.identifierConfiguration().get(entityType);
                assertNotNull(answered);
                assertEquals(identifiers.size(), answered.size());

                identifiers.forEach(identifier -> {
                    var policy = answered.get(identifier);
                    assertNotNull(policy);
                    assertTrue(policy.hideWhenNotSet());
                    assertFalse(policy.featured());
                    assertTrue(policy.canBeUpdated());
                });
            });
    }

    @Test
    public void shouldCompleteStoredIdentifierConfigurationWithDefaults() {
        // Given
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(configurationWithStoredPolicy(ApplicableEntityType.PERSON, "orcid",
                new IdentifierConfiguration(false, true, false))));

        // When
        var result = service.readConfigurationForSystem();

        // Then
        var orcid = result.identifierConfiguration().get(ApplicableEntityType.PERSON).get("orcid");
        assertFalse(orcid.hideWhenNotSet());
        assertTrue(orcid.featured());
        assertFalse(orcid.canBeUpdated());

        var apvnt = result.identifierConfiguration().get(ApplicableEntityType.PERSON).get("apvnt");
        assertTrue(apvnt.hideWhenNotSet());
        assertFalse(apvnt.featured());
        assertTrue(apvnt.canBeUpdated());
        assertNotNull(result.identifierConfiguration().get(ApplicableEntityType.DOCUMENT)
            .get("doi"));
    }

    @Test
    public void shouldIgnoreStoredIdentifierThatLeftTheCatalogue() {
        // Given
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(configurationWithStoredPolicy(ApplicableEntityType.PERSON,
                "identifierThatNoLongerExists", new IdentifierConfiguration(false, true, false))));

        // When
        var result = service.readConfigurationForSystem();

        // Then
        var person = result.identifierConfiguration().get(ApplicableEntityType.PERSON);
        assertFalse(person.containsKey("identifierThatNoLongerExists"));
        assertEquals(
            IdentifierConfigurationCatalogue.configurableIdentifiers()
                .get(ApplicableEntityType.PERSON).size(),
            person.size());
    }

    @Test
    public void shouldPersistIdentifierConfigurationOnSave() {
        // Given
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(Collections.emptyList());
        when(crisContextInformationRepository.save(any(CrisContextInformation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var configured = defaultIdentifierConfiguration();
        configured.get(ApplicableEntityType.DOCUMENT)
            .put("doi", new IdentifierConfigurationDTO(false, true, false));

        // When
        var result = service.saveConfiguration(dtoWithIdentifierConfiguration(configured));

        // Then
        var savedCaptor = ArgumentCaptor.forClass(CrisContextInformation.class);
        verify(crisContextInformationRepository).save(savedCaptor.capture());

        var storedDoi = savedCaptor.getValue().getIdentifierConfiguration()
            .get(ApplicableEntityType.DOCUMENT).get("doi");
        assertFalse(storedDoi.getHideWhenNotSet());
        assertTrue(storedDoi.getFeatured());
        assertFalse(storedDoi.getCanBeUpdated());

        var answeredDoi =
            result.identifierConfiguration().get(ApplicableEntityType.DOCUMENT).get("doi");
        assertTrue(answeredDoi.featured());
    }

    @Test
    public void shouldRejectIdentifierThatIsNotConfigurableForItsEntityType() {
        // Given
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(Collections.emptyList());

        var configured = defaultIdentifierConfiguration();
        configured.get(ApplicableEntityType.DOCUMENT)
            .put("orcid", new IdentifierConfigurationDTO(true, false, true));

        // When / Then
        assertThrows(IllegalArgumentException.class,
            () -> service.saveConfiguration(dtoWithIdentifierConfiguration(configured)));
        verify(crisContextInformationRepository, never()).save(any(CrisContextInformation.class));
    }

    @Test
    public void shouldReplaceStoredIdentifierConfigurationWholesale() {
        // Given
        var existing = configurationWithStoredPolicy(ApplicableEntityType.PERSON, "orcid",
            new IdentifierConfiguration(false, true, false));
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(existing));
        when(crisContextInformationRepository.save(any(CrisContextInformation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        var result = service.saveConfiguration(dtoWithIdentifierConfiguration(
            Map.of(ApplicableEntityType.DOCUMENT,
                Map.of("doi", new IdentifierConfigurationDTO(false, true, true)))));

        // Then
        assertFalse(existing.getIdentifierConfiguration()
            .getOrDefault(ApplicableEntityType.PERSON, Map.of()).containsKey("orcid"));

        var orcid = result.identifierConfiguration().get(ApplicableEntityType.PERSON).get("orcid");
        assertTrue(orcid.hideWhenNotSet());
        assertFalse(orcid.featured());
        assertTrue(orcid.canBeUpdated());
    }

    @Test
    public void shouldTreatMissingIdentifierConfigurationAsNoOverrides() {
        // Given
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(Collections.emptyList());
        when(crisContextInformationRepository.save(any(CrisContextInformation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        var result = service.saveConfiguration(dtoWithIdentifierConfiguration(null));

        // Then
        var savedCaptor = ArgumentCaptor.forClass(CrisContextInformation.class);
        verify(crisContextInformationRepository).save(savedCaptor.capture());
        assertTrue(savedCaptor.getValue().getIdentifierConfiguration().isEmpty());
        assertEquals(defaultIdentifierConfiguration(), result.identifierConfiguration());
    }

    @Test
    public void shouldPersistNationalIdentifierName() {
        // Given
        var languageTag = new LanguageTag();
        languageTag.setId(1);
        languageTag.setLanguageTag("EN");
        var resolved =
            Set.of(new MultiLingualContent(languageTag, "National researcher number", 1));

        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(Collections.emptyList());
        when(crisContextInformationRepository.save(any(CrisContextInformation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(resolved);

        var name = List.of(new MultilingualContentDTO(1, "EN", "National researcher number", 1));
        var dto = new CrisContextInformationDTO(true, true, true, true,
            ".*", ".*", ".*", ".*", License.CC0, defaultIdentifierConfiguration(), name, false);

        // When
        var result = service.saveConfiguration(dto);

        // Then
        var savedCaptor = ArgumentCaptor.forClass(CrisContextInformation.class);
        verify(crisContextInformationRepository).save(savedCaptor.capture());
        assertEquals(resolved, savedCaptor.getValue().getNationalIdentifierName());
        assertEquals(1, result.nationalIdentifierName().size());
        assertEquals("National researcher number",
            result.nationalIdentifierName().getFirst().getContent());
    }
}
