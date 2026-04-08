package rs.teslaris.core.service.impl.person;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.person.EmploymentPositionConverter;
import rs.teslaris.core.dto.person.involvement.EmploymentPositionDTO;
import rs.teslaris.core.model.person.EmploymentPositionHierarchy;
import rs.teslaris.core.repository.person.EmploymentPositionRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.person.EmploymentPositionService;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;
import rs.teslaris.core.util.search.StringUtil;

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
    @Transactional(readOnly = true)
    public Page<EmploymentPositionDTO> searchEmploymentPositions(Pageable pageable,
                                                                 String searchExpression,
                                                                 String languageTag) {
        if (searchExpression.equals("*")) {
            searchExpression = "";
        }

        return employmentPositionRepository.searchEmploymentPositions(
                StringUtil.performSimpleLatinPreprocessing(searchExpression), languageTag, pageable)
            .map(EmploymentPositionConverter::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public EmploymentPositionDTO readEmploymentPosition(Integer employmentPositionId) {
        return EmploymentPositionConverter.toDTO(findOne(employmentPositionId));
    }

    @Override
    @Transactional
    public EmploymentPositionDTO createEmploymentPosition(
        EmploymentPositionDTO employmentPositionDTO) {
        var newEmploymentPosition = new EmploymentPositionHierarchy();

        setCommonFields(newEmploymentPosition, employmentPositionDTO);

        return EmploymentPositionConverter.toDTO(save(newEmploymentPosition));
    }

    @Override
    @Transactional
    public void editEmploymentPosition(EmploymentPositionDTO employmentPositionDTO,
                                       Integer employmentPositionId) {
        var employmentPositionToUpdate = findOne(employmentPositionId);

        setCommonFields(employmentPositionToUpdate, employmentPositionDTO);

        save(employmentPositionToUpdate);
    }

    private void setCommonFields(EmploymentPositionHierarchy employmentPosition,
                                 EmploymentPositionDTO employmentPositionDTO) {
        employmentPosition.setName(
            multilingualContentService.getMultilingualContentAndSetDefaultsIfNonExistent(
                employmentPositionDTO.name()));
        employmentPosition.setDescription(
            multilingualContentService.getMultilingualContentAndSetDefaultsIfNonExistent(
                employmentPositionDTO.description()));

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
    @Transactional
    public void deleteEmploymentPosition(Integer employmentPositionId) {
        if (!employmentPositionRepository.getChildEmploymentPositions(employmentPositionId)
            .isEmpty()) {
            throw new ReferenceConstraintException("employmentPositionInUseMessage");
        }

        delete(employmentPositionId);
    }

    @Override
    @Transactional(readOnly = true)
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
