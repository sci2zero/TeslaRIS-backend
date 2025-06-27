package rs.teslaris.importer.dto;

import java.util.ArrayList;
import java.util.List;

public record AuthorCentricInstitutionHarvestRequestDTO(
    Integer institutionId,
    Boolean allAuthors,
    List<Integer> authorIds
) {
    public AuthorCentricInstitutionHarvestRequestDTO(Integer institutionId, Boolean allAuthors) {
        this(institutionId, allAuthors, new ArrayList<>());
    }
}

