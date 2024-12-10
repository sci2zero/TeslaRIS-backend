package rs.teslaris.core.service.impl.commontypes;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.dto.commontypes.ResearchAreaHierarchyDTO;
import rs.teslaris.core.dto.commontypes.ResearchAreaResponseDTO;
import rs.teslaris.core.dto.institution.ResearchAreaDTO;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.repository.commontypes.ResearchAreaRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.util.exceptionhandling.exception.ResearchAreaReferenceConstraintViolationException;

@Service
@RequiredArgsConstructor
@Transactional
public class ResearchAreaServiceImpl extends JPAServiceImpl<ResearchArea>
    implements ResearchAreaService {

    private final ResearchAreaRepository researchAreaRepository;

    private final MultilingualContentService multilingualContentService;

    @Override
    protected JpaRepository<ResearchArea, Integer> getEntityRepository() {
        return researchAreaRepository;
    }

    @Override
    public ResearchArea getReferenceToResearchAreaById(Integer id) {
        return id == null ? null : researchAreaRepository.getReferenceById(id);
    }

    @Override
    public Page<ResearchAreaResponseDTO> searchResearchAreas(Pageable pageable,
                                                             String searchExpression,
                                                             String languageTag) {
        if (searchExpression.equals("*")) {
            searchExpression = "";
        }

        return researchAreaRepository.searchResearchAreas(searchExpression, languageTag, pageable)
            .map(ResearchAreaConverter::toResponseDTO);
    }

    @Override
    public List<ResearchAreaHierarchyDTO> getResearchAreas() {
        return researchAreaRepository.getAllLeafs().stream().map(ResearchAreaConverter::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public ResearchAreaHierarchyDTO readResearchArea(
        Integer researchAreaId) {
        return ResearchAreaConverter.toDTO(findOne(researchAreaId));
    }

    @Override
    public List<ResearchAreaDTO> listResearchAreas() {
        return researchAreaRepository.findAll().stream().map((researchArea) -> new ResearchAreaDTO(
            researchArea.getId(),
            MultilingualContentConverter.getMultilingualContentDTO(researchArea.getName()),
            MultilingualContentConverter.getMultilingualContentDTO(researchArea.getDescription()),
            Objects.nonNull(researchArea.getSuperResearchArea()) ?
                researchArea.getSuperResearchArea().getId() : null)).collect(Collectors.toList());
    }

    @Override
    public ResearchArea createResearchArea(ResearchAreaDTO researchAreaDTO) {
        var newResearchArea = new ResearchArea();

        setCommonFields(newResearchArea, researchAreaDTO);

        return save(newResearchArea);
    }

    @Override
    public void editResearchArea(ResearchAreaDTO researchAreaDTO, Integer researchAreaId) {
        var researchAreaToUpdate = getReferenceToResearchAreaById(researchAreaId);

        researchAreaToUpdate.setSuperResearchArea(null);
        setCommonFields(researchAreaToUpdate, researchAreaDTO);

        save(researchAreaToUpdate);
    }

    @Override
    public void deleteResearchArea(Integer researchAreaId) {
        if (researchAreaRepository.isSuperArea(researchAreaId) ||
            researchAreaRepository.isResearchedBySomeone(researchAreaId) ||
            researchAreaRepository.isResearchedInMonograph(researchAreaId) ||
            researchAreaRepository.isResearchedInThesis(researchAreaId)) {
            throw new ResearchAreaReferenceConstraintViolationException(
                "Research area with id " + researchAreaId + " cannot be deleted as it is in use.");
        }

        this.delete(researchAreaId);
    }

    @Override
    public List<ResearchArea> getResearchAreasByIds(List<Integer> ids) {
        return researchAreaRepository.findAllById(ids);
    }

    private void setCommonFields(ResearchArea researchArea, ResearchAreaDTO researchAreaDTO) {
        researchArea.setName(
            multilingualContentService.getMultilingualContentAndSetDefaultsIfNonExistent(
                researchAreaDTO.getName()));
        researchArea.setDescription(
            multilingualContentService.getMultilingualContentAndSetDefaultsIfNonExistent(
                researchAreaDTO.getDescription()));
        researchArea.setSuperResearchArea(
            getReferenceToResearchAreaById(researchAreaDTO.getSuperResearchAreaId()));
    }
}
