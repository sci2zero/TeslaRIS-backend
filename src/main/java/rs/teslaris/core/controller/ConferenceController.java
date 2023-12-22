package rs.teslaris.core.controller;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
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
import rs.teslaris.core.dto.document.ConferenceBasicAdditionDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.service.interfaces.document.ConferenceService;

@RestController
@RequestMapping("/api/conference")
@RequiredArgsConstructor
public class ConferenceController {

    private final ConferenceService conferenceService;


    @GetMapping
    public Page<ConferenceDTO> readAll(Pageable pageable) {
        return conferenceService.readAllConferences(pageable);
    }

    @GetMapping("/simple-search")
    Page<EventIndex> searchConferences(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        Pageable pageable) {
        return conferenceService.searchConferences(tokens, pageable);
    }

    @GetMapping("/{conferenceId}")
    public ConferenceDTO readConference(@PathVariable Integer conferenceId) {
        return conferenceService.readConference(conferenceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_CONFERENCES')")
    @Idempotent
    public ConferenceDTO createConference(@RequestBody @Valid ConferenceDTO conferenceDTO) {
        var newConference = conferenceService.createConference(conferenceDTO);
        conferenceDTO.setId(newConference.getId());
        return conferenceDTO;
    }

    @PostMapping("/basic")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_CONFERENCES')")
    @Idempotent
    public ConferenceBasicAdditionDTO createConferenceBasic(
        @RequestBody @Valid ConferenceBasicAdditionDTO conferenceDTO) {
        var newConference = conferenceService.createConference(conferenceDTO);
        conferenceDTO.setId(newConference.getId());
        return conferenceDTO;
    }

    @PutMapping("/{conferenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_CONFERENCES')")
    public void updateConference(@PathVariable Integer conferenceId,
                                 @RequestBody @Valid ConferenceDTO conferenceDTO) {
        conferenceService.updateConference(conferenceDTO, conferenceId);
    }

    @DeleteMapping("/{conferenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_CONFERENCES')")
    public void deleteConference(@PathVariable Integer conferenceId) {
        conferenceService.deleteConference(conferenceId);
    }
}
