package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.PublicConfigurationDTO;
import rs.teslaris.core.model.commontypes.BrandingInformation;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.commontypes.BrandingInformationRepository;
import rs.teslaris.core.service.impl.commontypes.PublicConfigurationServiceImpl;

@SpringBootTest
public class PublicConfigurationServiceTest {

    @Mock
    private BrandingInformationRepository brandingInformationRepository;

    @InjectMocks
    private PublicConfigurationServiceImpl publicConfigurationService;


    @Test
    public void shouldComposePublicConfigurationFromBranding() {
        var branding = new BrandingInformation();
        var languageTag = new LanguageTag();
        languageTag.setLanguageTag("EN");
        var title = new MultiLingualContent();
        title.setLanguage(languageTag);
        title.setContent("TeslaRIS");
        branding.setTitle(Set.of(title));
        branding.setDescription(Set.of(title));
        branding.setLogoServerName("logo.png");
        branding.setLastModification(new Date(1_700_000_000_000L));

        when(brandingInformationRepository.findAll()).thenReturn(List.of(branding));

        var result = publicConfigurationService.readPublicConfiguration();

        assertEquals(PublicConfigurationDTO.SCHEMA_VERSION, result.schemaVersion());
        assertNotNull(result.updatedAt());
        assertEquals("TeslaRIS", result.branding().title().getFirst().getContent());
        assertEquals("branding/logo?v=" + result.updatedAt(), result.branding().logoUrl());
        assertNull(result.branding().backgroundUrl());
    }

    @Test
    public void shouldReturnDefaultsWhenBrandingIsMissing() {
        when(brandingInformationRepository.findAll()).thenReturn(List.of());

        var result = publicConfigurationService.readPublicConfiguration();

        assertEquals(PublicConfigurationDTO.SCHEMA_VERSION, result.schemaVersion());
        assertNull(result.branding().logoUrl());
        assertEquals(0, result.branding().title().size());
    }
}
