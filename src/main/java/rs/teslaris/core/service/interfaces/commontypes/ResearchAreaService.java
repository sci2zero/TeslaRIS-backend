package rs.teslaris.core.service.interfaces.commontypes;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.institution.ResearchAreaDTO;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface ResearchAreaService extends JPAService<ResearchArea> {

    ResearchArea getReferenceToResearchAreaById(Integer id);

    List<rs.teslaris.core.dto.commontypes.ResearchAreaDTO> getResearchAreas();

    List<ResearchAreaDTO> listResearchAreas();

    ResearchArea createResearchArea(ResearchAreaDTO researchAreaDTO);

    void editResearchArea(ResearchAreaDTO researchAreaDTO, Integer researchAreaId);

    void deleteResearchArea(Integer researchAreaId);

    List<ResearchArea> getResearchAreasByIds(List<Integer> id);
}
