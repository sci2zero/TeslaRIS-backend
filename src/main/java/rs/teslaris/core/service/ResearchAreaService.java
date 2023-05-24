package rs.teslaris.core.service;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.institution.ResearchAreaDTO;
import rs.teslaris.core.model.commontypes.ResearchArea;

@Service
public interface ResearchAreaService {

    ResearchArea getReferenceToResearchAreaById(Integer id);

    ResearchArea createResearchArea(ResearchAreaDTO researchAreaDTO);

    void editResearchArea(ResearchAreaDTO researchAreaDTO, Integer researchAreaId);

    void deleteResearchArea(Integer researchAreaId);
}
