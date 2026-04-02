package rs.teslaris.core.service.impl.person;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.person.EmploymentPositionConverter;
import rs.teslaris.core.dto.person.involvement.EmploymentPositionDTO;
import rs.teslaris.core.model.person.EmploymentPositionHierarchy;
import rs.teslaris.core.repository.person.EmploymentPositionRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.person.EmploymentPositionService;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;

@Service
@RequiredArgsConstructor
@Traceable
public class EmploymentPositionServiceImpl extends JPAServiceImpl<EmploymentPositionHierarchy>
    implements EmploymentPositionService {

    private final EmploymentPositionRepository employmentPositionRepository;

    private final MultilingualContentService multilingualContentService;


    @Override
    protected JpaRepository<EmploymentPositionHierarchy, Integer> getEntityRepository() {
        return employmentPositionRepository;
    }

    @Override
    public EmploymentPositionHierarchy createEmploymentPosition(
        EmploymentPositionDTO employmentPositionDTO) {
        var newEmploymentPosition = new EmploymentPositionHierarchy();

        setCommonFields(newEmploymentPosition, employmentPositionDTO);

        return save(newEmploymentPosition);
    }

    @Override
    public void editEmploymentPosition(EmploymentPositionDTO employmentPositionDTO,
                                       Integer employmentPositionId) {
        var employmentPositionToUpdate = findOne(employmentPositionId);

        setCommonFields(employmentPositionToUpdate, employmentPositionDTO);

        save(employmentPositionToUpdate);
    }

    private void setCommonFields(EmploymentPositionHierarchy employmentPosition,
                                 EmploymentPositionDTO employmentPositionDTO) {
        employmentPosition.setName(
            multilingualContentService.getMultilingualContent(employmentPositionDTO.name()));
        employmentPosition.setProcessedName(employmentPositionDTO.processedName());
        employmentPosition.setSchemeName(employmentPositionDTO.schemeName());

        if (Objects.nonNull(employmentPositionDTO.superEmploymentPositionId())) {
            employmentPosition.setSuperEmploymentPosition(findOne(
                employmentPositionDTO.superEmploymentPositionId()));
        } else {
            employmentPosition.setSuperEmploymentPosition(null);
        }
    }

    @Override
    public void deleteEmploymentPosition(Integer employmentPositionId) {
        if (!employmentPositionRepository.getChildEmploymentPositions(employmentPositionId)
            .isEmpty()) {
            throw new ReferenceConstraintException("employmentPositionInUseMessage");
        }

        delete(employmentPositionId);
    }

    @Override
    public List<EmploymentPositionDTO> getChildEmploymentPositions(Integer parentId) {
        List<EmploymentPositionHierarchy> fetchedEmploymentPositions;
        if (Objects.isNull(parentId) || parentId == 0) {
            fetchedEmploymentPositions =
                employmentPositionRepository.getTopLevelEmploymentPositions();
        } else {
            fetchedEmploymentPositions =
                employmentPositionRepository.getChildEmploymentPositions(parentId);
        }

        return fetchedEmploymentPositions.stream()
            .map(EmploymentPositionConverter::toDTO)
            .collect(Collectors.toList());
    }
}
