package rs.teslaris.core.service.impl.commontypes;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.BrandingInformationDTO;
import rs.teslaris.core.model.commontypes.BrandingInformation;
import rs.teslaris.core.repository.commontypes.BrandingInformationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.BrandingInformationService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;

@Service
@RequiredArgsConstructor
@Transactional
public class BrandingInformationServiceImpl extends JPAServiceImpl<BrandingInformation>
    implements BrandingInformationService {

    private final BrandingInformationRepository brandingInformationRepository;

    private final MultilingualContentService multilingualContentService;


    @Override
    protected JpaRepository<BrandingInformation, Integer> getEntityRepository() {
        return brandingInformationRepository;
    }

    @Override
    public BrandingInformationDTO readBrandingInformation() {
        var brandingInformation = findOne(1);

        return new BrandingInformationDTO(MultilingualContentConverter.getMultilingualContentDTO(
            brandingInformation.getTitle()),
            MultilingualContentConverter.getMultilingualContentDTO(
                brandingInformation.getDescription()));
    }

    @Override
    public void updateBrandingInformation(BrandingInformationDTO brandingInformationDTO) {
        BrandingInformation brandingInformation;

        var savedBrandingInformation = brandingInformationRepository.findAll();
        if (savedBrandingInformation.isEmpty()) {
            brandingInformation = new BrandingInformation();
        } else {
            brandingInformation = savedBrandingInformation.getFirst();
        }

        brandingInformation.setTitle(
            multilingualContentService.getMultilingualContent(brandingInformationDTO.title()));
        brandingInformation.setDescription(multilingualContentService.getMultilingualContent(
            brandingInformationDTO.description()));

        save(brandingInformation);
    }
}
