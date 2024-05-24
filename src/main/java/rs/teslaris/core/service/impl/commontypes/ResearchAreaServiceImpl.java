package rs.teslaris.core.service.impl.commontypes;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
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
    public List<rs.teslaris.core.dto.commontypes.ResearchAreaDTO> getResearchAreas() {
        return researchAreaRepository.getAllLeafs().stream().map(ResearchAreaConverter::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<ResearchAreaDTO> listResearchAreas() {
        return researchAreaRepository.findAll().stream().map((researchArea) -> new ResearchAreaDTO(
            researchArea.getId(),
            MultilingualContentConverter.getMultilingualContentDTO(researchArea.getName()),
            MultilingualContentConverter.getMultilingualContentDTO(researchArea.getDescription()),
            researchArea.getSuperResearchArea().getId())).collect(Collectors.toList());
    }

    @Override
    public ResearchArea createResearchArea(ResearchAreaDTO researchAreaDTO) {
        var newResearchArea = new ResearchArea();
        newResearchArea.setName(
            multilingualContentService.getMultilingualContent(researchAreaDTO.getName()));
        newResearchArea.setDescription(
            multilingualContentService.getMultilingualContent(researchAreaDTO.getDescription()));
        newResearchArea.setSuperResearchArea(
            getReferenceToResearchAreaById(researchAreaDTO.getSuperResearchAreaId()));

        return this.save(newResearchArea);
    }

    @Override
    public void editResearchArea(ResearchAreaDTO researchAreaDTO, Integer researchAreaId) {
        var reserchAreaToUpdate = getReferenceToResearchAreaById(researchAreaId);

        reserchAreaToUpdate.getName().clear();
        reserchAreaToUpdate.setName(
            multilingualContentService.getMultilingualContent(researchAreaDTO.getName()));
        reserchAreaToUpdate.getDescription().clear();
        reserchAreaToUpdate.setDescription(
            multilingualContentService.getMultilingualContent(researchAreaDTO.getDescription()));

        reserchAreaToUpdate.setSuperResearchArea(
            getReferenceToResearchAreaById(researchAreaDTO.getSuperResearchAreaId()));

        this.save(reserchAreaToUpdate);
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
}
