package rs.teslaris.core.controller.document;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.service.interfaces.document.MonographService;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.signposting.FairSignpostingL1Utility;

@RestController
@RequestMapping("/api/monograph")
@RequiredArgsConstructor
@Traceable
public class MonographController {

    private final MonographService monographService;

    private final DeduplicationService deduplicationService;


    @GetMapping("/{documentId}")
    public ResponseEntity<MonographDTO> readMonograph(@PathVariable Integer documentId) {
        var dto = monographService.readMonographById(documentId);

        return ResponseEntity.ok()
            .headers(FairSignpostingL1Utility.constructHeaders(dto, "/api/monograph"))
            .body(dto);
    }

    @GetMapping("/old-id/{oldId}")
    public MonographDTO readMonographByOldId(@PathVariable Integer oldId) {
        return monographService.readMonographByOldId(oldId);
    }

    @GetMapping("/simple-search")
    public Page<DocumentPublicationIndex> simpleSearch(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        @RequestParam(value = "onlyBooks", required = false) Boolean onlyBooks) {
        StringUtil.sanitizeTokens(tokens);
        return monographService.searchMonographs(tokens, onlyBooks);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck("CREATE")
    @Idempotent
    public MonographDTO createMonograph(@RequestBody @Valid MonographDTO monographDTO) {
        var savedMonograph = monographService.createMonograph(monographDTO, true);
        monographDTO.setId(savedMonograph.getId());
        return monographDTO;
    }

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editMonograph(@PathVariable Integer documentId,
                              @RequestBody @Valid MonographDTO monographDTO) {
        monographService.editMonograph(documentId, monographDTO);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteMonograph(@PathVariable Integer documentId) {
        monographService.deleteMonograph(documentId);
        deduplicationService.deleteSuggestion(documentId, EntityType.PUBLICATION);
    }

    @DeleteMapping("/force/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('FORCE_DELETE_ENTITIES')")
    public void forceDeleteMonograph(@PathVariable Integer documentId) {
        monographService.forceDeleteMonograph(documentId);
        deduplicationService.deleteSuggestion(documentId, EntityType.PUBLICATION);
    }

    @GetMapping("/identifier-usage/{documentId}")
    @PublicationEditCheck
    public boolean checkIdentifierUsage(@PathVariable Integer documentId,
                                        @RequestParam String identifier) {
        return monographService.isIdentifierInUse(identifier, documentId);
    }
}
