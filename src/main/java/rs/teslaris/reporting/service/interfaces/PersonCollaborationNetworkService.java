package rs.teslaris.reporting.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.reporting.dto.CollaborationNetworkDTO;

@Service
public interface PersonCollaborationNetworkService {

    CollaborationNetworkDTO findCollaborationNetwork(Integer authorId, int depth);
}
