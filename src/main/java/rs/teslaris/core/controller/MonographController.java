package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.service.interfaces.document.MonographService;
import rs.teslaris.core.util.search.StringUtil;

@RestController
@RequestMapping("/api/monograph")
@RequiredArgsConstructor
public class MonographController {

    private final MonographService monographService;


    @GetMapping("/{documentId}")
    public MonographDTO readMonograph(@PathVariable Integer documentId) {
        return monographService.readMonographById(documentId);
    }

    @GetMapping("/simple-search")
    public Page<DocumentPublicationIndex> simpleSearch(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens) {
        StringUtil.sanitizeTokens(tokens);
        return monographService.searchMonographs(tokens);
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
    }
}
