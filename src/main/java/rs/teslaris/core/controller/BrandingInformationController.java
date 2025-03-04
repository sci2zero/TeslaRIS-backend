package rs.teslaris.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.commontypes.BrandingInformationDTO;
import rs.teslaris.core.service.interfaces.commontypes.BrandingInformationService;

@RestController
@RequestMapping("/api/branding")
@RequiredArgsConstructor
public class BrandingInformationController {

    private final BrandingInformationService brandingInformationService;

    @GetMapping
    public BrandingInformationDTO readBrandingInformation() {
        return brandingInformationService.readBrandingInformation();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('UPDATE_BRANDING_INFORMATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateBrandingInformation(
        @RequestBody BrandingInformationDTO brandingInformationDTO) {
        brandingInformationService.updateBrandingInformation(brandingInformationDTO);
    }
}
