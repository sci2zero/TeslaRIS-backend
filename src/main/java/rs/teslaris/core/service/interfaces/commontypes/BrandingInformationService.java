package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.BrandingInformationDTO;
import rs.teslaris.core.model.commontypes.BrandingInformation;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface BrandingInformationService extends JPAService<BrandingInformation> {

    BrandingInformationDTO readBrandingInformation();

    void updateBrandingInformation(BrandingInformationDTO brandingInformation);
}
