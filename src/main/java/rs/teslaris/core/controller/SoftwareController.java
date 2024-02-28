package rs.teslaris.core.controller;

import javax.validation.Valid;
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

    @GetMapping("/{publicationId}")
    public SoftwareDTO readSoftware(
        @PathVariable Integer publicationId) {
        return softwareService.readSoftwareById(publicationId);
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

    @PutMapping("/{publicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editSoftware(@PathVariable Integer publicationId,
                             @RequestBody @Valid SoftwareDTO software) {
        softwareService.editSoftware(publicationId, software);
    }

    @DeleteMapping("/{publicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteSoftware(@PathVariable Integer publicationId) {
        softwareService.deleteSoftware(publicationId);
    }
}