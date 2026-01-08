package rs.teslaris.core.util.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.service.interfaces.commontypes.BrandingInformationService;

@Component
public class BrandingInformationUtil {

    private static BrandingInformationService brandingInformationService;


    @Autowired
    public BrandingInformationUtil(BrandingInformationService brandingInformationService) {
        BrandingInformationUtil.brandingInformationService = brandingInformationService;
    }

    public static String getSystemName(String language) {
        var brandingTitle = brandingInformationService.readBrandingInformation().title();
        return brandingTitle.stream()
            .filter(t -> t.getLanguageTag().equalsIgnoreCase(language))
            .findFirst()
            .or(() -> brandingTitle.stream().findFirst())
            .map(MultilingualContentDTO::getContent)
            .orElse("TeslaRIS");
    }
}
