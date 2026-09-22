package rs.teslaris.core.controller.document;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.IntellectualPropertyDTO;
import rs.teslaris.core.service.interfaces.document.IntellectualPropertyService;
import rs.teslaris.core.util.signposting.FairSignpostingL1Utility;

@RestController
@RequestMapping("api/intellectual-property")
@RequiredArgsConstructor
@Traceable
public class IntellectualPropertyController {

    private final IntellectualPropertyService intellectualPropertyService;

    @GetMapping("/{documentId}")
    public ResponseEntity<IntellectualPropertyDTO> readIntellectualProperty(
        @PathVariable Integer documentId) {
        var dto = intellectualPropertyService.readIntellectualPropertyById(documentId);

        return ResponseEntity.ok()
            .headers(FairSignpostingL1Utility.constructHeaders(dto, "/api/intellectualProperty"))
            .body(dto);
    }

    @GetMapping("/old-id/{oldId}")
    public IntellectualPropertyDTO readIntellectualPropertyByOldId(@PathVariable Integer oldId) {
        return intellectualPropertyService.readIntellectualPropertyByOldId(oldId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck("CREATE")
    @Idempotent
    public IntellectualPropertyDTO createIntellectualProperty(
        @RequestBody @Valid IntellectualPropertyDTO intellectualProperty) {
        var savedIntellectualProperty =
            intellectualPropertyService.createIntellectualProperty(intellectualProperty, true);
        intellectualProperty.setId(savedIntellectualProperty.getId());
        return intellectualProperty;
    }

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editIntellectualProperty(@PathVariable Integer documentId,
                                         @RequestBody @Valid
                                         IntellectualPropertyDTO intellectualProperty) {
        intellectualPropertyService.editIntellectualProperty(documentId, intellectualProperty);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteIntellectualProperty(@PathVariable Integer documentId) {
        intellectualPropertyService.deleteIntellectualProperty(documentId);
    }
}
