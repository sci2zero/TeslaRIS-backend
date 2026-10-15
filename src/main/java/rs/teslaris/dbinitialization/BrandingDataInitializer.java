package rs.teslaris.dbinitialization;

import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.commontypes.BrandingInformation;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.commontypes.BrandingInformationRepository;

/**
 * Seeds the default branding information shown before an installation configures its own.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class BrandingDataInitializer {

    private final BrandingInformationRepository brandingInformationRepository;


    public void initializeDefaultBranding(LanguageTag serbianTag, LanguageTag englishTag) {
        var brandingInformation = new BrandingInformation();
        brandingInformation.setTitle(new HashSet<>(Set.of(
            new MultiLingualContent(serbianTag, "CRIS UNS", 1),
            new MultiLingualContent(englishTag, "CRIS UNS", 2)
        )));
        brandingInformation.setDescription(new HashSet<>(Set.of(
            new MultiLingualContent(serbianTag,
                "CRIS UNS je informacioni sistem naučno-istraživačke delatnosti Univerziteta u Novom Sadu. U ovom sistemu možete pronaći informacije o istraživačima, organizacionim jedinicama i objavljenim rezultatima ovog univerziteta. Informacioni sistem je barizan na TeslaRIS open-source platformi.",
                1),
            new MultiLingualContent(englishTag,
                "CRIS UNS is the information system of scientific research activities at the University of Novi Sad. In this system, you can find information about researchers, organisation units, and scientific results of this university. The information system is based on the TeslaRIS open-source platform.",
                2)
        )));
        brandingInformationRepository.save(brandingInformation);
    }
}
