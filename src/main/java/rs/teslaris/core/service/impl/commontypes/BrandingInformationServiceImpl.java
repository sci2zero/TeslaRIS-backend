package rs.teslaris.core.service.impl.commontypes;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.BrandingInformationDTO;
import rs.teslaris.core.model.commontypes.BrandingInformation;
import rs.teslaris.core.repository.commontypes.BrandingInformationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.BrandingInformationService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.util.files.ImageUtil;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
public class BrandingInformationServiceImpl extends JPAServiceImpl<BrandingInformation>
    implements BrandingInformationService {

    private final BrandingInformationRepository brandingInformationRepository;

    private final MultilingualContentService multilingualContentService;

    private final FileService fileService;


    @Override
    protected JpaRepository<BrandingInformation, Integer> getEntityRepository() {
        return brandingInformationRepository;
    }

    @Override
    public BrandingInformationDTO readBrandingInformation() {
        var brandingInformation = findCurrent().orElse(null);
        if (brandingInformation == null) {
            return new BrandingInformationDTO(List.of(), List.of());
        }

        return new BrandingInformationDTO(
            MultilingualContentConverter.getMultilingualContentDTO(
                brandingInformation.getTitle()),
            MultilingualContentConverter.getMultilingualContentDTO(
                brandingInformation.getDescription())
        );
    }

    @Override
    public void updateBrandingInformation(BrandingInformationDTO brandingInformationDTO) {
        var brandingInformation = findCurrent().orElseGet(BrandingInformation::new);

        brandingInformation.setTitle(
            multilingualContentService.getMultilingualContent(brandingInformationDTO.title()));
        brandingInformation.setDescription(multilingualContentService.getMultilingualContent(
            brandingInformationDTO.description()));

        save(brandingInformation);
    }

    @Override
    public void updateLogo(MultipartFile file) throws IOException {
        storeBrandingImage(file, true);
    }

    @Override
    public void removeLogo() {
        var brandingInformation = findCurrent().orElse(null);
        if (brandingInformation == null ||
            Objects.isNull(brandingInformation.getLogoServerName())) {
            return;
        }

        fileService.delete(brandingInformation.getLogoServerName());
        brandingInformation.setLogoServerName(null);
        save(brandingInformation);
    }

    @Override
    public void updateBackground(MultipartFile file) throws IOException {
        storeBrandingImage(file, false);
    }

    @Override
    public void removeBackground() {
        var brandingInformation = findCurrent().orElse(null);
        if (brandingInformation == null ||
            Objects.isNull(brandingInformation.getBackgroundServerName())) {
            return;
        }

        fileService.delete(brandingInformation.getBackgroundServerName());
        brandingInformation.setBackgroundServerName(null);
        save(brandingInformation);
    }

    @Override
    @Transactional(readOnly = true)
    public String getLogoServerFilename() {
        return findCurrent()
            .map(BrandingInformation::getLogoServerName)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public String getBackgroundServerFilename() {
        return findCurrent()
            .map(BrandingInformation::getBackgroundServerName)
            .orElse(null);
    }

    private void storeBrandingImage(MultipartFile file, boolean logo) throws IOException {
        if (file == null || file.isEmpty() || ImageUtil.isMIMETypeInvalid(file, false)) {
            throw new IllegalArgumentException("mimeTypeValidationFailed");
        }

        var brandingInformation = findCurrent().orElseGet(BrandingInformation::new);
        var previousFilename = logo
            ? brandingInformation.getLogoServerName()
            : brandingInformation.getBackgroundServerName();

        if (Objects.nonNull(previousFilename)) {
            fileService.delete(previousFilename);
        }

        var serverFilename = fileService.store(file, UUID.randomUUID().toString());
        if (logo) {
            brandingInformation.setLogoServerName(serverFilename);
        } else {
            brandingInformation.setBackgroundServerName(serverFilename);
        }

        save(brandingInformation);
    }

    private Optional<BrandingInformation> findCurrent() {
        var saved = brandingInformationRepository.findAll();
        if (saved.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(saved.getFirst());
    }
}
