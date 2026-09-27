package rs.teslaris.core.controller.utility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.BrandingInformationDTO;
import rs.teslaris.core.service.interfaces.commontypes.BrandingInformationService;
import rs.teslaris.core.service.interfaces.document.FileService;

@RestController
@RequestMapping("/api/branding")
@RequiredArgsConstructor
@Traceable
public class BrandingInformationController {

    private final BrandingInformationService brandingInformationService;

    private final FileService fileService;


    @GetMapping
    public BrandingInformationDTO readBrandingInformation() {
        return brandingInformationService.readBrandingInformation();
    }

    @GetMapping("/logo")
    public ResponseEntity<InputStreamResource> serveLogo() throws IOException {
        return serveBrandingAsset(brandingInformationService.getLogoServerFilename());
    }

    @GetMapping("/background")
    public ResponseEntity<InputStreamResource> serveBackground() throws IOException {
        return serveBrandingAsset(brandingInformationService.getBackgroundServerFilename());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('UPDATE_BRANDING_INFORMATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateBrandingInformation(
        @RequestBody BrandingInformationDTO brandingInformationDTO) {
        brandingInformationService.updateBrandingInformation(brandingInformationDTO);
    }

    @PatchMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('UPDATE_BRANDING_INFORMATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLogo(@RequestParam("file") MultipartFile file) throws IOException {
        brandingInformationService.updateLogo(file);
    }

    @DeleteMapping("/logo")
    @PreAuthorize("hasAuthority('UPDATE_BRANDING_INFORMATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeLogo() {
        brandingInformationService.removeLogo();
    }

    @PatchMapping(value = "/background", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('UPDATE_BRANDING_INFORMATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateBackground(@RequestParam("file") MultipartFile file) throws IOException {
        brandingInformationService.updateBackground(file);
    }

    @DeleteMapping("/background")
    @PreAuthorize("hasAuthority('UPDATE_BRANDING_INFORMATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBackground() {
        brandingInformationService.removeBackground();
    }

    private ResponseEntity<InputStreamResource> serveBrandingAsset(String filename)
        throws IOException {
        if (Objects.isNull(filename) || filename.isBlank()) {
            return ResponseEntity.noContent().build();
        }

        var file = fileService.loadAsResource(filename);
        var resource = new InputStreamResource(file);

        var contentType = file.response().contentType();
        if (Objects.isNull(contentType)) {
            contentType = Files.probeContentType(Path.of(filename));
        }
        if (Objects.isNull(contentType)) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .contentLength(Objects.requireNonNullElse(file.response().contentLength(), 0L))
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
            .header(HttpHeaders.ETAG, file.response().eTag())
            .body(resource);
    }
}
