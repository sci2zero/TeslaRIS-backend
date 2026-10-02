package rs.teslaris.core.service.impl.commontypes;

import java.io.IOException;
import java.util.ArrayList;
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
import rs.teslaris.core.converter.commontypes.GeoLocationConverter;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.person.PostalAddressConverter;
import rs.teslaris.core.dto.commontypes.BrandingInformationDTO;
import rs.teslaris.core.model.commontypes.BrandingInformation;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.repository.commontypes.BrandingInformationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.BrandingInformationService;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.util.files.ImageUtil;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.restoration.RestorationSupport;
import rs.teslaris.core.util.search.StringUtil;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
public class BrandingInformationServiceImpl extends JPAServiceImpl<BrandingInformation>
    implements BrandingInformationService {

    private final BrandingInformationRepository brandingInformationRepository;

    private final MultilingualContentService multilingualContentService;

    private final FileService fileService;

    private final CountryService countryService;


    @Override
    protected JpaRepository<BrandingInformation, Integer> getEntityRepository() {
        return brandingInformationRepository;
    }

    @Override
    public BrandingInformationDTO readBrandingInformation() {
        var brandingInformation = findCurrent().orElse(null);
        if (brandingInformation == null) {
            return new BrandingInformationDTO(List.of(), List.of(), null, null, null);
        }

        return new BrandingInformationDTO(
            MultilingualContentConverter.getMultilingualContentDTO(
                brandingInformation.getTitle()),
            MultilingualContentConverter.getMultilingualContentDTO(
                brandingInformation.getDescription()),
            GeoLocationConverter.toDTO(brandingInformation.getLocation()),
            PostalAddressConverter.toDto(brandingInformation.getPostalAddress()),
            brandingInformation.getPhoneNumber());
    }

    @Override
    public void updateBrandingInformation(BrandingInformationDTO brandingInformationDTO) {
        var brandingInformation = findCurrent().orElseGet(BrandingInformation::new);

        brandingInformation.setTitle(
            multilingualContentService.getMultilingualContent(brandingInformationDTO.title()));
        brandingInformation.setDescription(multilingualContentService.getMultilingualContent(
            brandingInformationDTO.description()));

        brandingInformation.setPhoneNumber(brandingInformationDTO.phoneNumber());

        setPostalAddressInfo(brandingInformation, brandingInformationDTO);
        setLocationInfo(brandingInformation, brandingInformationDTO);

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

    /**
     * The postal address is an embeddable holding orphanRemoval collections, so it is never
     * replaced or set to null: Hibernate fails the flush once a managed collection is no longer
     * reachable from the owning entity. Everything is cleared and refilled in place instead.
     */
    private void setPostalAddressInfo(BrandingInformation brandingInformation,
                                      BrandingInformationDTO brandingInformationDTO) {
        if (Objects.isNull(brandingInformation.getPostalAddress())) {
            brandingInformation.setPostalAddress(new PostalAddress());
        }

        var postalAddress = brandingInformation.getPostalAddress();
        var postalAddressDTO = brandingInformationDTO.postalAddress();

        postalAddress.getStreetAndNumber().clear();
        postalAddress.getCity().clear();
        postalAddress.getState().clear();

        if (Objects.isNull(postalAddressDTO)) {
            postalAddress.setPostalNumber(null);
            postalAddress.setCountry(null);
            return;
        }

        postalAddress.getStreetAndNumber().addAll(
            multilingualContentService.getMultilingualContent(
                postalAddressDTO.getStreetAndNumber()));

        postalAddress.getCity().addAll(
            multilingualContentService.getMultilingualContent(postalAddressDTO.getCity()));

        postalAddress.getState().addAll(
            multilingualContentService.getMultilingualContent(postalAddressDTO.getState()));

        postalAddress.setPostalNumber(postalAddressDTO.getPostalNumber());

        if (Objects.nonNull(postalAddressDTO.getCountryId()) &&
            postalAddressDTO.getCountryId() > 0) {
            postalAddress.setCountry(
                RestorationSupport.resolveOptional(postalAddressDTO.getCountryId(), countryService,
                    countryService::findOne, "postalAddress.countryId",
                    "restoreCountryMissingMessage"));
        } else {
            postalAddress.setCountry(null);
        }
    }

    private void setLocationInfo(BrandingInformation brandingInformation,
                                 BrandingInformationDTO brandingInformationDTO) {
        if (Objects.isNull(brandingInformationDTO.location())) {
            brandingInformation.setLocation(null);
            return;
        }

        brandingInformation.setLocation(
            GeoLocationConverter.fromDTO(brandingInformationDTO.location()));

        if (StringUtil.valueExists(brandingInformation.getLocation().getAddress())) {
            return;
        }

        // No address was picked on the map, so derive a readable one from the postal address.
        var postalAddress = brandingInformation.getPostalAddress();
        var parts = new ArrayList<String>();

        for (var content : List.of(postalAddress.getStreetAndNumber(), postalAddress.getCity(),
            postalAddress.getState())) {
            var value = StringUtil.getStringContent(content, LanguageAbbreviations.SERBIAN);
            if (StringUtil.valueExists(value)) {
                parts.add(value);
            }
        }

        if (!parts.isEmpty()) {
            brandingInformation.getLocation().setAddress(String.join(", ", parts));
        }
    }

    private Optional<BrandingInformation> findCurrent() {
        var saved = brandingInformationRepository.findAll();
        if (saved.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(saved.getFirst());
    }
}
