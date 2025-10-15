package rs.teslaris.reporting.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.document.DocumentCollaborationService;
import rs.teslaris.reporting.dto.CollaborationNetworkDTO;
import rs.teslaris.reporting.utility.CollaborationType;

@Service
public interface PersonCollaborationNetworkService extends DocumentCollaborationService {

    CollaborationNetworkDTO findCollaborationNetwork(Integer authorId, Integer depth,
                                                     CollaborationType collaborationType,
                                                     Integer yearFrom, Integer yearTo);
}
