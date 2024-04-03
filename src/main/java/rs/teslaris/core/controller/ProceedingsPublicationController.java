package rs.teslaris.core.controller;

import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("api/proceedings-publication")
@RequiredArgsConstructor
public class ProceedingsPublicationController {

    private final ProceedingsPublicationService proceedingsPublicationService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping("/{publicationId}")
    public ProceedingsPublicationDTO readProceedingsPublication(
        @PathVariable Integer publicationId) {
        return proceedingsPublicationService.readProceedingsPublicationById(publicationId);
    }

    @GetMapping("/event/{eventId}/my-publications")
    public List<ProceedingsPublicationResponseDTO> readAuthorsProceedingsPublicationsForEvent(
        @PathVariable Integer eventId, @RequestHeader("Authorization") String bearerToken) {
        return proceedingsPublicationService.findAuthorsProceedingsForEvent(eventId,
            userService.getPersonIdForUser(
                tokenUtil.extractUserIdFromToken(bearerToken.split(" ")[1])));
    }

    @GetMapping("/event/{eventId}")
    public Page<DocumentPublicationIndex> readAllProceedingsPublicationsForEvent(
        @PathVariable Integer eventId, Pageable pageable) {
        return proceedingsPublicationService.findProceedingsForEvent(eventId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck("CREATE")
    @Idempotent
    public ProceedingsPublicationDTO createProceedingsPublication(
        @RequestBody @Valid ProceedingsPublicationDTO proceedingsPublication) {
        var savedProceedingsPublication =
            proceedingsPublicationService.createProceedingsPublication(proceedingsPublication,
                true);
        proceedingsPublication.setId(savedProceedingsPublication.getId());
        return proceedingsPublication;
    }

    @PutMapping("/{publicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editProceedingsPublication(@PathVariable Integer publicationId,
                                           @RequestBody
                                           @Valid
                                           ProceedingsPublicationDTO proceedingsPublication) {
        proceedingsPublicationService.editProceedingsPublication(publicationId,
            proceedingsPublication);
    }

    @DeleteMapping("/{publicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteProceedingsPublication(@PathVariable Integer publicationId) {
        proceedingsPublicationService.deleteProceedingsPublication(publicationId);
    }
}
