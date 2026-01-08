package rs.teslaris.core.controller.publicationseries;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.ReorderContributionRequestDTO;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesService;

@RestController
@RequestMapping("/api/publication-series")
@RequiredArgsConstructor
@Traceable
public class PublicationSeriesController {

    private final PublicationSeriesService publicationSeriesService;

    @PatchMapping("/{publicationSeriesId}/reorder-contribution/{contributionId}")
    @PreAuthorize("hasAuthority('EDIT_PUBLICATION_SERIES')")
    void reorderEventContributions(@PathVariable Integer publicationSeriesId,
                                   @PathVariable Integer contributionId,
                                   @RequestBody ReorderContributionRequestDTO reorderRequest) {
        publicationSeriesService.reorderPublicationSeriesContributions(publicationSeriesId,
            contributionId, reorderRequest.getOldContributionOrderNumber(),
            reorderRequest.getNewContributionOrderNumber());
    }
}
