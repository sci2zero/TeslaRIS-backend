package rs.teslaris.thesislibrary.service.impl;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.PromotionException;
import rs.teslaris.thesislibrary.converter.PromotionConverter;
import rs.teslaris.thesislibrary.dto.PromotionDTO;
import rs.teslaris.thesislibrary.model.Promotion;
import rs.teslaris.thesislibrary.repository.PromotionRepository;
import rs.teslaris.thesislibrary.service.interfaces.PromotionService;

@Service
@RequiredArgsConstructor
@Traceable
public class PromotionServiceImpl extends JPAServiceImpl<Promotion> implements PromotionService {

    private final PromotionRepository promotionRepository;

    private final MultilingualContentService multilingualContentService;

    private final OrganisationUnitService organisationUnitService;


    @Override
    protected JpaRepository<Promotion, Integer> getEntityRepository() {
        return promotionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PromotionDTO> getAllPromotions(Integer institutionId, Pageable pageable) {
        if (Objects.nonNull(institutionId) && institutionId > 0) {
            return promotionRepository.findAll(institutionId, pageable)
                .map(PromotionConverter::toDTO);
        }

        return promotionRepository.findAll(pageable).map(PromotionConverter::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionDTO> getPromotionsBasedOnStatus(Integer institutionId, boolean finished) {
        if (Objects.nonNull(institutionId)) {
            return promotionRepository.getPromotionsBasedOnStatus(institutionId, finished).stream()
                .map(PromotionConverter::toDTO).toList();
        }

        return promotionRepository.getPromotionsBasedOnStatus(finished).stream()
            .map(PromotionConverter::toDTO).toList();
    }

    @Override
    @Transactional
    public Promotion createPromotion(PromotionDTO promotionDTO) {
        var newPromotion = new Promotion();

        setCommonFields(newPromotion, promotionDTO);

        return save(newPromotion);
    }

    @Override
    @Transactional
    public Promotion migratePromotion(PromotionDTO promotionDTO) {
        var newPromotion = new Promotion();

        setCommonFields(newPromotion, promotionDTO);
        newPromotion.setFinished(promotionDTO.getFinished());

        return save(newPromotion);
    }

    @Override
    @Transactional
    public void updatePromotion(Integer promotionId, PromotionDTO promotionDTO) {
        var promotionToUpdate = findOne(promotionId);

        if (promotionToUpdate.getFinished()) {
            throw new PromotionException("Finished promotions cannot be updated.");
        }

        setCommonFields(promotionToUpdate, promotionDTO);

        save(promotionToUpdate);
    }

    private void setCommonFields(Promotion promotion, PromotionDTO promotionDTO) {
        promotion.setPromotionDate(promotionDTO.getPromotionDate());
        promotion.setPromotionTime(promotionDTO.getPromotionTime());
        promotion.setPlaceOrVenue(promotionDTO.getPlaceOrVenue());
        promotion.setDescription(
            multilingualContentService.getMultilingualContent(promotionDTO.getDescription()));
        promotion.setInstitution(organisationUnitService.findOne(promotionDTO.getInstitutionId()));
    }

    @Override
    @Transactional
    public void deletePromotion(Integer promotionId) {
        if (promotionRepository.hasPromotableEntries(promotionId)) {
            throw new PromotionException("Promotion has entries.");
        }

        promotionRepository.delete(findOne(promotionId));
    }
}
