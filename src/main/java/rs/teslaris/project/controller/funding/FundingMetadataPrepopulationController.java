package rs.teslaris.project.controller.funding;


import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.project.dto.funding.PrepopulatedFundingMetadataDTO;
import rs.teslaris.project.service.interfaces.commontypes.FundingMetadataPrepopulationService;

@RestController
@RequestMapping("/api/funding-metadata-prepopulation")
@RequiredArgsConstructor
public class FundingMetadataPrepopulationController {

    private final FundingMetadataPrepopulationService prepopulationService;

    @GetMapping
    @PreAuthorize("hasAuthority('HARVEST_IDF_METADATA')")
    public PrepopulatedFundingMetadataDTO getFundingMetadataForDoi(@RequestParam String doi) {
        return prepopulationService.fetchFundingDataForDoi(doi);
    }
}