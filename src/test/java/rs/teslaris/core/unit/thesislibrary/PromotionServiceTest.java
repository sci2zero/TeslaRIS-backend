package rs.teslaris.core.unit.thesislibrary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.PromotionException;
import rs.teslaris.thesislibrary.dto.PromotionDTO;
import rs.teslaris.thesislibrary.model.Promotion;
import rs.teslaris.thesislibrary.repository.PromotionRepository;
import rs.teslaris.thesislibrary.service.impl.PromotionServiceImpl;

@SpringBootTest
public class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private PromotionServiceImpl service;


    @Test
    void shouldCreatePromotion() {
        // Given
        var dto =
            new PromotionDTO(null, LocalDate.now(), LocalTime.now(), "Somewhere", List.of(), 1);
        Promotion saved = new Promotion();
        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(promotionRepository.save(any(Promotion.class))).thenReturn(saved);

        // When
        Promotion result = service.createPromotion(dto);

        // Then
        assertNotNull(result);
        verify(promotionRepository).save(any(Promotion.class));
    }

    @Test
    void shouldUpdatePromotion() {
        // Given
        var id = 1;
        var existing = new Promotion();
        var dto = new PromotionDTO(1, LocalDate.now(), LocalTime.now(), "Somewhere", List.of(), 1);

        when(promotionRepository.findById(id)).thenReturn(Optional.of(existing));
        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(new MultiLingualContent()));

        // When
        service.updatePromotion(id, dto);

        // Then
        verify(promotionRepository).save(existing);
    }

    @Test
    void shouldThrowExceptionWhenDeletingPromotionWithEntries() {
        // Given
        var id = 1;
        when(promotionRepository.hasPromotableEntries(id)).thenReturn(true);

        // Then
        assertThrows(PromotionException.class, () -> service.deletePromotion(id));
    }

    @Test
    void shouldDeleteWhenDeletingPromotionWithoutEntries() {
        // Given
        var id = 1;
        Promotion promo = new Promotion();

        when(promotionRepository.hasPromotableEntries(id)).thenReturn(false);
        when(promotionRepository.findById(id)).thenReturn(Optional.of(promo));

        // When
        service.deletePromotion(id);

        // Then
        verify(promotionRepository).delete(promo);
    }

    @Test
    void shouldReturnDTOListWhenGettingNonFinishedPromotions() {
        // Given
        var dummyMC = new MultiLingualContent();
        dummyMC.setLanguage(new LanguageTag());
        var p = new Promotion();
        p.setPromotionDate(LocalDate.now());
        p.setPromotionTime(LocalTime.NOON);
        p.setPlaceOrVenue("Main Hall");
        p.setInstitution(new OrganisationUnit());
        p.setDescription(Set.of(dummyMC));

        when(promotionRepository.getNonFinishedPromotions()).thenReturn(List.of(p));

        // When
        var result = service.getNonFinishedPromotions(null);

        // Then
        assertEquals(1, result.size());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void shouldReturnAllPromotions(Integer userId) {
        // Given
        var pageable = PageRequest.of(0, 2);
        var promotion1 = new Promotion();
        promotion1.setInstitution(new OrganisationUnit());
        var promotion2 = new Promotion();
        promotion2.setInstitution(new OrganisationUnit());
        var promotions = List.of(promotion1, promotion2);
        var promotionPage = new PageImpl<>(promotions, pageable, 2);

        when(promotionRepository.findAll(pageable)).thenReturn(promotionPage);
        when(promotionRepository.findAll(anyInt(), eq(pageable))).thenReturn(promotionPage);

        // When
        var result = service.getAllPromotions(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        if (userId < 1) {
            verify(promotionRepository).findAll(pageable);
        } else {
            verify(promotionRepository).findAll(anyInt(), eq(pageable));
        }
    }
}
