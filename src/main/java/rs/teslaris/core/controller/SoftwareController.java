package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.service.interfaces.document.SoftwareService;

@RestController
@RequestMapping("api/software")
@RequiredArgsConstructor
public class SoftwareController {

    private final SoftwareService softwareService;

    @GetMapping("/{documentId}")
    public SoftwareDTO readSoftware(
        @PathVariable Integer documentId) {
        return softwareService.readSoftwareById(documentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck("CREATE")
    @Idempotent
    public SoftwareDTO createSoftware(@RequestBody @Valid SoftwareDTO software) {
        var savedSoftware = softwareService.createSoftware(software, true);
        software.setId(savedSoftware.getId());
        return software;
    }

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editSoftware(@PathVariable Integer documentId,
                             @RequestBody @Valid SoftwareDTO software) {
        softwareService.editSoftware(documentId, software);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteSoftware(@PathVariable Integer documentId) {
        softwareService.deleteSoftware(documentId);
    }
}
