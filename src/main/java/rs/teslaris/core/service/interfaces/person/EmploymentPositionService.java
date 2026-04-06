package rs.teslaris.core.service.interfaces.person;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.person.involvement.EmploymentPositionDTO;
import rs.teslaris.core.model.person.EmploymentPositionHierarchy;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface EmploymentPositionService extends JPAService<EmploymentPositionHierarchy> {

    Page<EmploymentPositionDTO> searchEmploymentPositions(Pageable pageable,
                                                          String searchExpression,
                                                          String languageTag);

    EmploymentPositionDTO readEmploymentPosition(Integer employmentPositionId);

    EmploymentPositionDTO createEmploymentPosition(
        EmploymentPositionDTO employmentPositionDTO);

    void editEmploymentPosition(EmploymentPositionDTO employmentPositionDTO,
                                Integer employmentPositionId);

    void deleteEmploymentPosition(Integer employmentPositionId);

    List<EmploymentPositionDTO> getChildEmploymentPositions(Integer parentId);
}
