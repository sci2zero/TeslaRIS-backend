package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import rs.teslaris.core.dto.commontypes.BrandingInformationDTO;
import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.model.commontypes.BrandingInformation;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.repository.commontypes.BrandingInformationRepository;
import rs.teslaris.core.service.impl.commontypes.BrandingInformationServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.util.files.ImageUtil;
import rs.teslaris.core.util.language.LanguageAbbreviations;

@SpringBootTest
public class BrandingInformationServiceTest {

    @Mock
    private BrandingInformationRepository brandingInformationRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private FileService fileService;

    @Mock
    private CountryService countryService;

    @InjectMocks
    private BrandingInformationServiceImpl brandingInformationService;


    @Test
    public void shouldReturnBrandingInformationDTOWhenBrandingInformationExists() {
        // given
        var brandingInformation = new BrandingInformation();
        var dummyMc = new MultiLingualContent();
        dummyMc.setLanguage(new LanguageTag());
        brandingInformation.setTitle(Set.of(dummyMc));
        brandingInformation.setDescription(Set.of(dummyMc));

        when(brandingInformationRepository.findAll()).thenReturn(List.of(brandingInformation));

        // when
        var result = brandingInformationService.readBrandingInformation();

        // then
        assertNotNull(result);
        assertEquals(1, result.title().size());
        verify(multilingualContentService, never()).getMultilingualContent(any());
    }

    @Test
    public void shouldReturnDefaultBrandingWhenNoneExists() {
        when(brandingInformationRepository.findAll()).thenReturn(Collections.emptyList());

        var result = brandingInformationService.readBrandingInformation();

        assertNotNull(result);
        assertEquals(0, result.title().size());
        assertEquals(0, result.description().size());
    }

    @Test
    public void shouldUpdateBrandingInformationWhenDTOIsProvided() {
        // given
        var brandingInformationDTO = new BrandingInformationDTO(
            List.of(new MultilingualContentDTO()), List.of(new MultilingualContentDTO()), null,
            null, null
        );

        var existingBrandingInformation = new BrandingInformation();
        existingBrandingInformation.setTitle(Set.of(new MultiLingualContent()));
        existingBrandingInformation.setDescription(Set.of(new MultiLingualContent()));

        when(brandingInformationRepository.findAll()).thenReturn(
            List.of(existingBrandingInformation));
        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));

        // when
        brandingInformationService.updateBrandingInformation(brandingInformationDTO);

        // then
        verify(brandingInformationRepository, times(1)).findAll();
        verify(multilingualContentService, times(2)).getMultilingualContent(any());
    }

    @Test
    public void shouldDeriveLocationAddressFromPostalAddressWhenMapAddressIsMissing() {
        // given
        var postalAddress = new PostalAddressDTO();
        postalAddress.setStreetAndNumber(List.of(new MultilingualContentDTO()));
        postalAddress.setCity(List.of(new MultilingualContentDTO()));
        postalAddress.setState(List.of(new MultilingualContentDTO()));

        var brandingInformationDTO = new BrandingInformationDTO(
            List.of(new MultilingualContentDTO()), List.of(new MultilingualContentDTO()),
            new GeoLocationDTO(20.0, 45.0, null), postalAddress, "+381 21 000 000"
        );

        var existingBrandingInformation = new BrandingInformation();
        when(brandingInformationRepository.findAll()).thenReturn(
            List.of(existingBrandingInformation));

        var city = new MultiLingualContent();
        city.setLanguage(serbianLanguageTag());
        city.setContent("Novi Sad");
        when(multilingualContentService.getMultilingualContent(any())).thenReturn(Set.of(city));

        // when
        brandingInformationService.updateBrandingInformation(brandingInformationDTO);

        // then
        var location = existingBrandingInformation.getLocation();
        assertNotNull(location);
        assertEquals(20.0, location.getLongitude());
        assertEquals(45.0, location.getLatitude());
        assertEquals("Novi Sad, Novi Sad, Novi Sad", location.getAddress());
        assertNotNull(existingBrandingInformation.getPostalAddress());
    }

    @Test
    public void shouldKeepMapAddressWhenProvided() {
        // given
        var brandingInformationDTO = new BrandingInformationDTO(
            List.of(new MultilingualContentDTO()), List.of(new MultilingualContentDTO()),
            new GeoLocationDTO(20.0, 45.0, "Trg Dositeja Obradovica 6"), null, null
        );

        var existingBrandingInformation = new BrandingInformation();
        when(brandingInformationRepository.findAll()).thenReturn(
            List.of(existingBrandingInformation));
        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));

        // when
        brandingInformationService.updateBrandingInformation(brandingInformationDTO);

        // then
        assertEquals("Trg Dositeja Obradovica 6",
            existingBrandingInformation.getLocation().getAddress());
        assertTrue(existingBrandingInformation.getPostalAddress().getCity().isEmpty());
    }

    @Test
    public void shouldClearPostalAddressInPlaceWhenDTOOmitsIt() {
        // given
        var existingBrandingInformation = new BrandingInformation();
        var existingPostalAddress = new PostalAddress();
        existingPostalAddress.getCity().add(new MultiLingualContent());
        existingPostalAddress.getStreetAndNumber().add(new MultiLingualContent());
        existingPostalAddress.getState().add(new MultiLingualContent());
        existingPostalAddress.setPostalNumber("21000");
        existingBrandingInformation.setPostalAddress(existingPostalAddress);

        var brandingInformationDTO = new BrandingInformationDTO(
            List.of(new MultilingualContentDTO()), List.of(new MultilingualContentDTO()), null,
            null, null
        );

        when(brandingInformationRepository.findAll()).thenReturn(
            List.of(existingBrandingInformation));
        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));

        // when
        brandingInformationService.updateBrandingInformation(brandingInformationDTO);

        // then the very same embeddable and collection instances must survive, otherwise
        // Hibernate fails the flush on the orphanRemoval collections
        assertSame(existingPostalAddress, existingBrandingInformation.getPostalAddress());
        assertTrue(existingPostalAddress.getCity().isEmpty());
        assertTrue(existingPostalAddress.getStreetAndNumber().isEmpty());
        assertTrue(existingPostalAddress.getState().isEmpty());
        assertNull(existingPostalAddress.getPostalNumber());
    }

    private LanguageTag serbianLanguageTag() {
        var languageTag = new LanguageTag();
        languageTag.setLanguageTag(LanguageAbbreviations.SERBIAN);
        return languageTag;
    }

    @Test
    public void shouldCreateNewBrandingInformationWhenNoneExists() {
        // given
        var brandingInformationDTO = new BrandingInformationDTO(
            List.of(new MultilingualContentDTO()), List.of(new MultilingualContentDTO()), null,
            null, null
        );

        when(brandingInformationRepository.findAll()).thenReturn(Collections.emptyList());
        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));

        // when
        brandingInformationService.updateBrandingInformation(brandingInformationDTO);

        // then
        verify(brandingInformationRepository, times(1)).findAll();
        verify(multilingualContentService, times(2)).getMultilingualContent(any());
    }

    @Test
    public void shouldRejectInvalidLogoUpload() {
        var file = new MockMultipartFile("file", "logo.txt", "text/plain", "nope".getBytes());

        try (MockedStatic<ImageUtil> mockedStatic = mockStatic(ImageUtil.class)) {
            mockedStatic.when(() -> ImageUtil.isMIMETypeInvalid(file, false)).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                () -> brandingInformationService.updateLogo(file));
            verify(fileService, never()).store(any(), any());
        }
    }

    @Test
    public void shouldStoreLogoWhenValidImageIsProvided() throws IOException {
        var file = new MockMultipartFile("file", "logo.png", "image/png", new byte[] {1, 2, 3});
        var brandingInformation = new BrandingInformation();

        when(brandingInformationRepository.findAll()).thenReturn(List.of(brandingInformation));
        when(fileService.store(any(), anyString())).thenReturn("stored.png");

        try (MockedStatic<ImageUtil> mockedStatic = mockStatic(ImageUtil.class)) {
            mockedStatic.when(() -> ImageUtil.isMIMETypeInvalid(file, false)).thenReturn(false);

            brandingInformationService.updateLogo(file);
        }

        verify(fileService).store(any(), anyString());
        assertEquals("stored.png", brandingInformation.getLogoServerName());
    }
}
