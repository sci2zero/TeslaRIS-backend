package rs.teslaris.reporting.dto;

import java.util.List;

public record CollaborationNetworkDTO(
    List<PersonNode> nodes,
    List<CollaborationLink> links
) {
}
