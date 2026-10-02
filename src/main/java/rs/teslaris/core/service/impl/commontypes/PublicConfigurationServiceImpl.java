package rs.teslaris.core.service.impl.commontypes;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.PublicBrandingDTO;
import rs.teslaris.core.dto.commontypes.PublicConfigurationDTO;
import rs.teslaris.core.model.commontypes.BrandingInformation;
import rs.teslaris.core.repository.commontypes.BrandingInformationRepository;
import rs.teslaris.core.service.interfaces.commontypes.PublicConfigurationService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Traceable
public class PublicConfigurationServiceImpl implements PublicConfigurationService {

    private final BrandingInformationRepository brandingInformationRepository;


    @Override
    public PublicConfigurationDTO readPublicConfiguration() {
        var branding = brandingInformationRepository.findAll().stream().findFirst().orElse(null);
        var version = latestModification(branding);

        return new PublicConfigurationDTO(
            PublicConfigurationDTO.SCHEMA_VERSION,
            version,
            toPublicBranding(branding, version)
        );
    }

    private PublicBrandingDTO toPublicBranding(BrandingInformation branding, String version) {
        if (branding == null) {
            return new PublicBrandingDTO(List.of(), List.of(), null, null, null, null);
        }

        return new PublicBrandingDTO(
            MultilingualContentConverter.getMultilingualContentDTO(branding.getTitle()),
            MultilingualContentConverter.getMultilingualContentDTO(branding.getDescription()),
            toAssetUrl(branding.getLogoServerName(), "branding/logo", version),
            toAssetUrl(branding.getBackgroundServerName(), "branding/background", version),
            branding.getChromeTheme(),
            branding.getHeroTheme()
        );
    }

    private String toAssetUrl(String serverFilename, String path, String version) {
        if (Objects.isNull(serverFilename) || serverFilename.isBlank()) {
            return null;
        }
        return path + "?v=" + version;
    }

    private String latestModification(BrandingInformation branding) {
        var brandingDate = branding == null ? null : branding.getLastModification();
        var latestMillis = toEpochMillis(brandingDate);
        if (latestMillis <= 0) {
            return Instant.EPOCH.toString();
        }
        return Instant.ofEpochMilli(latestMillis).toString();
    }

    private long toEpochMillis(Date date) {
        return date == null ? 0L : date.getTime();
    }
}
