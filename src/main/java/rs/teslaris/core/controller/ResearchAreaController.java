package rs.teslaris.core.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.dto.institution.ResearchAreaDTO;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;

@RestController
@RequestMapping("/api/research-area")
@RequiredArgsConstructor
public class ResearchAreaController {

    private final ResearchAreaService researchAreaService;

    @GetMapping
    public List<rs.teslaris.core.dto.commontypes.ResearchAreaDTO> getResearchAreas() {
        return researchAreaService.getResearchAreas();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_RESEARCH_AREAS')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public ResearchAreaDTO createResearchArea(@RequestBody ResearchAreaDTO researchArea) {
        var newResearchArea = researchAreaService.createResearchArea(researchArea);
        researchArea.setId(newResearchArea.getId());
        return researchArea;
    }

    @PutMapping("/{researchAreaId}")
    @PreAuthorize("hasAuthority('EDIT_RESEARCH_AREAS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editResearchArea(@RequestBody ResearchAreaDTO researchArea,
                                 @PathVariable Integer researchAreaId) {
        researchAreaService.editResearchArea(researchArea, researchAreaId);
    }

    @DeleteMapping("/{researchAreaId}")
    @PreAuthorize("hasAuthority('EDIT_RESEARCH_AREAS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResearchArea(@PathVariable Integer researchAreaId) {
        researchAreaService.deleteResearchArea(researchAreaId);
    }
}
