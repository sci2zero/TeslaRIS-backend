package rs.teslaris.core.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.ResearchAreaHierarchyDTO;
import rs.teslaris.core.dto.commontypes.ResearchAreaNodeDTO;
import rs.teslaris.core.dto.commontypes.ResearchAreaResponseDTO;
import rs.teslaris.core.dto.institution.ResearchAreaDTO;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;

@RestController
@RequestMapping("/api/research-area")
@RequiredArgsConstructor
@Traceable
public class ResearchAreaController {

    private final ResearchAreaService researchAreaService;


    @GetMapping("/search")
    public Page<ResearchAreaResponseDTO> searchResearchAreas(Pageable pageable,
                                                             @RequestParam("tokens")
                                                             List<String> tokens,
                                                             @RequestParam("lang")
                                                             String language) {
        return researchAreaService.searchResearchAreas(pageable, Strings.join(tokens, ' '),
            language.toUpperCase());
    }

    @GetMapping
    public List<ResearchAreaHierarchyDTO> getResearchAreas() {
        return researchAreaService.getResearchAreas();
    }

    @GetMapping("/{researchAreaId}")
    public ResearchAreaHierarchyDTO getResearchArea(
        @PathVariable Integer researchAreaId) {
        return researchAreaService.readResearchArea(researchAreaId);
    }

    @GetMapping("/children/{parentId}")
    public List<ResearchAreaNodeDTO> getChildResearchAreas(@PathVariable Integer parentId) {
        return researchAreaService.getChildResearchAreas(parentId);
    }

    @GetMapping("/list")
    public List<ResearchAreaDTO> listResearchAreas() {
        return researchAreaService.listResearchAreas();
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
