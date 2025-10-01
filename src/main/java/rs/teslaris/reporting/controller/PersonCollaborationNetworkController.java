package rs.teslaris.reporting.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.reporting.dto.CollaborationNetworkDTO;
import rs.teslaris.reporting.service.interfaces.PersonCollaborationNetworkService;

@RestController
@RequestMapping("/api/collaboration-network")
@RequiredArgsConstructor
public class PersonCollaborationNetworkController {

    private final PersonCollaborationNetworkService personCollaborationNetworkService;


    @GetMapping("/{personId}")
    public CollaborationNetworkDTO getPersonCollaborationNetwork(@PathVariable Integer personId) {
        return personCollaborationNetworkService.findCollaborationNetwork(personId, 3);
    }
}
