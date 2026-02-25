package rs.teslaris.core.controller.document;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.service.interfaces.document.ThesisResearchOutputService;

@RestController
@RequestMapping("/api/thesis/research-output")
@RequiredArgsConstructor
@Traceable
public class ThesisResearchOutputController {

    private final ThesisResearchOutputService thesisResearchOutputService;

    @GetMapping("/{documentId}")
    public Page<DocumentPublicationIndex> readThesisResearchOutput(@PathVariable Integer documentId,
                                                                   Pageable pageable) {
        return thesisResearchOutputService.readResearchOutputsForThesis(documentId, pageable);
    }

    @PatchMapping("/add/{documentId}/{researchOutputId}")
    @PublicationEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addThesisResearchOutput(@PathVariable Integer documentId,
                                        @PathVariable Integer researchOutputId) {
        thesisResearchOutputService.addResearchOutput(documentId, researchOutputId);
    }

    @PatchMapping("/remove/{documentId}/{researchOutputId}")
    @PublicationEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeThesisResearchOutput(@PathVariable Integer documentId,
                                           @PathVariable Integer researchOutputId) {
        thesisResearchOutputService.removeResearchOutput(documentId, researchOutputId);
    }
}
