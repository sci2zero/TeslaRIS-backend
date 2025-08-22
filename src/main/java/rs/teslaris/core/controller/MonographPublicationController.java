package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.service.interfaces.document.MonographPublicationService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.signposting.FairSignpostingL1Utility;

@RestController
@RequestMapping("/api/monograph-publication")
@RequiredArgsConstructor
@Traceable
public class MonographPublicationController {

    private final MonographPublicationService monographPublicationService;

    private final UserService userService;

    private final JwtUtil tokenUtil;


    @GetMapping("/{documentId}")
    public ResponseEntity<MonographPublicationDTO> readMonographPublication(
        @PathVariable Integer documentId) {
        var dto = monographPublicationService.readMonographPublicationById(documentId);

        return ResponseEntity.ok()
            .headers(FairSignpostingL1Utility.constructHeaders(dto, "/api/monograph-publication"))
            .body(dto);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck("CREATE")
    @Idempotent
    public MonographPublicationDTO createMonographPublication(
        @RequestBody @Valid MonographPublicationDTO monographPublicationDTO) {
        var savedMonographPublication =
            monographPublicationService.createMonographPublication(monographPublicationDTO, true);
        monographPublicationDTO.setId(savedMonographPublication.getId());
        return monographPublicationDTO;
    }

    @GetMapping("/monograph/{monographId}/my-publications")
    public List<DocumentPublicationIndex> readAuthorsMonographPublicationsForMonograph(
        @PathVariable Integer monographId, @RequestHeader("Authorization") String bearerToken) {
        return monographPublicationService.findAuthorsPublicationsForMonograph(monographId,
            userService.getPersonIdForUser(
                tokenUtil.extractUserIdFromToken(bearerToken)));
    }

    @GetMapping("/monograph/{monographId}")
    public Page<DocumentPublicationIndex> readAllMonographPublicationsForMonograph(
        @PathVariable Integer monographId, Pageable pageable) {
        return monographPublicationService.findAllPublicationsForMonograph(monographId, pageable);
    }

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editMonographPublication(@PathVariable Integer documentId,
                                         @RequestBody
                                         @Valid MonographPublicationDTO monographPublicationDTO) {
        monographPublicationService.editMonographPublication(documentId, monographPublicationDTO);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteMonographPublication(@PathVariable Integer documentId) {
        monographPublicationService.deleteMonographPublication(documentId);
    }
}
