package rs.teslaris.project.controller.project;


import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.project.dto.project.PrepopulatedProjectMetadataDTO;
import rs.teslaris.project.service.interfaces.commontypes.ProjectMetadataPrepopulationService;

@RestController
@RequestMapping("/api/project-metadata-prepopulation")
@RequiredArgsConstructor
public class ProjectMetadataPrepopulationController {

    private final ProjectMetadataPrepopulationService prepopulationService;

    @GetMapping
    @PreAuthorize("hasAuthority('HARVEST_IDF_METADATA')")
    public PrepopulatedProjectMetadataDTO getFundingMetadataForDoi(@RequestParam String doi) {
        return prepopulationService.fetchProjectDataForDoi(doi);
    }
}