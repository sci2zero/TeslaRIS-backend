package rs.teslaris.reporting.controller.visualizations;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.reporting.dto.CollaborationNetworkDTO;
import rs.teslaris.reporting.service.interfaces.PersonCollaborationNetworkService;
import rs.teslaris.reporting.utility.CollaborationType;

@RestController
@RequestMapping("/api/collaboration-network")
@RequiredArgsConstructor
public class PersonCollaborationNetworkController {

    private final PersonCollaborationNetworkService personCollaborationNetworkService;


    @GetMapping("/{personId}")
    public CollaborationNetworkDTO getPersonCollaborationNetwork(@PathVariable Integer personId,
                                                                 @RequestParam Integer depth,
                                                                 @RequestParam
                                                                 CollaborationType collaborationType,
                                                                 @RequestParam Integer yearFrom,
                                                                 @RequestParam Integer yearTo) {
        return personCollaborationNetworkService.findCollaborationNetwork(personId, depth,
            collaborationType, yearFrom, yearTo);
    }

    @GetMapping("/works/{sourcePersonId}/{targetPersonId}")
    public Page<DocumentPublicationIndex> getPublicationsForCollaboration(
        @PathVariable Integer sourcePersonId,
        @PathVariable Integer targetPersonId,
        @RequestParam Integer yearFrom, @RequestParam Integer yearTo,
        @RequestParam CollaborationType collaborationType,
        Pageable pageable) {
        return personCollaborationNetworkService.findPublicationsForCollaboration(sourcePersonId,
            targetPersonId, collaborationType.name(), yearFrom, yearTo, pageable);
    }
}
