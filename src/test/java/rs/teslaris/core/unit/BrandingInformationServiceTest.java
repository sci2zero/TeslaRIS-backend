package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.BrandingInformation;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.commontypes.BrandingInformationRepository;
import rs.teslaris.core.service.impl.commontypes.BrandingInformationServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.util.files.ImageUtil;

@SpringBootTest
public class BrandingInformationServiceTest {

    @Mock
    private BrandingInformationRepository brandingInformationRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private FileService fileService;

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
            List.of(new MultilingualContentDTO()), List.of(new MultilingualContentDTO())
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
    public void shouldCreateNewBrandingInformationWhenNoneExists() {
        // given
        var brandingInformationDTO = new BrandingInformationDTO(
            List.of(new MultilingualContentDTO()), List.of(new MultilingualContentDTO())
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
