package rs.teslaris.core.service.interfaces.commontypes;

import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.dto.commontypes.BrandingInformationDTO;
import rs.teslaris.core.model.commontypes.BrandingInformation;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface BrandingInformationService extends JPAService<BrandingInformation> {

    BrandingInformationDTO readBrandingInformation();

    void updateBrandingInformation(BrandingInformationDTO brandingInformation);

    void updateLogo(MultipartFile file) throws IOException;

    void removeLogo();

    void updateBackground(MultipartFile file) throws IOException;

    void removeBackground();

    String getLogoServerFilename();

    String getBackgroundServerFilename();
}
