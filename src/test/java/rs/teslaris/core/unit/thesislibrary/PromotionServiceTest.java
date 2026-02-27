package rs.teslaris.core.unit.thesislibrary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.PromotionException;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;
import rs.teslaris.core.util.session.SessionUtil;
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

    static Stream<Arguments> searchFilterProvider() {
        return Stream.of(
            Arguments.of(0, false),
            Arguments.of(1, false),
            Arguments.of(0, true),
            Arguments.of(1, true)
        );
    }

    @Test
    void shouldCreatePromotion() {
        // Given
        var dto =
            new PromotionDTO(null, LocalDate.now(), LocalTime.now(), "Somewhere", List.of(), 1,
                false, null);
        Promotion saved = new Promotion();
        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(promotionRepository.save(any(Promotion.class))).thenReturn(saved);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(any())).thenReturn(
            List.of(1));

        // When
        try (MockedStatic<SessionUtil> sessionUtilMock = mockStatic(SessionUtil.class)) {
            sessionUtilMock.when(SessionUtil::getLoggedInUser)
                .thenReturn(new User() {{
                    setOrganisationUnit(new OrganisationUnit() {{
                        setId(1);
                    }});
                }});

            var result = service.createPromotion(dto);

            // Then
            assertNotNull(result);
            verify(promotionRepository).save(any(Promotion.class));
        }
    }

    @Test
    void shouldThrowWhenInstitutionNotInSubHierarchy() {
        // Given
        var dto = new PromotionDTO(
            null,
            LocalDate.now(),
            LocalTime.now(),
            "Somewhere",
            List.of(),
            999, // NOT in allowed sub-hierarchy
            false,
            null
        );

        var user = new User();
        var organisationUnit = new OrganisationUnit();
        organisationUnit.setId(1);
        user.setOrganisationUnit(organisationUnit);

        when(organisationUnitService
            .getOrganisationUnitIdsFromSubHierarchy(1))
            .thenReturn(List.of(1));

        try (MockedStatic<SessionUtil> sessionUtilMock = mockStatic(SessionUtil.class)) {
            sessionUtilMock.when(SessionUtil::isUserLoggedInAndAdmin)
                .thenReturn(false);

            sessionUtilMock.when(SessionUtil::getLoggedInUser)
                .thenReturn(user);

            // When & Then
            assertThrows(ReferenceConstraintException.class,
                () -> service.createPromotion(dto));

            verify(promotionRepository, never())
                .save(any(Promotion.class));
        }
    }

    @Test
    void shouldUpdatePromotion() {
        // Given
        var id = 1;
        var existing = new Promotion();
        var dto =
            new PromotionDTO(1, LocalDate.now(), LocalTime.now(), "Somewhere", List.of(), 1, false,
                null);

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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnDTOListWhenGettingPromotionsBasedOnStatus(boolean finished) {
        // Given
        var dummyMC = new MultiLingualContent();
        dummyMC.setLanguage(new LanguageTag());
        var p = new Promotion();
        p.setPromotionDate(LocalDate.now());
        p.setPromotionTime(LocalTime.NOON);
        p.setPlaceOrVenue("Main Hall");
        p.setInstitution(new OrganisationUnit());
        p.setDescription(Set.of(dummyMC));

        when(promotionRepository.getPromotionsBasedOnStatus(finished)).thenReturn(List.of(p));

        // When
        var result = service.getPromotionsBasedOnStatus(null, finished);

        // Then
        assertEquals(1, result.size());
    }

    @ParameterizedTest
    @MethodSource("searchFilterProvider")
    void shouldReturnAllPromotions(int institutionId, boolean nonFinishedOnly) {
        // Given
        var pageable = PageRequest.of(0, 2);
        var promotion1 = new Promotion();
        promotion1.setInstitution(new OrganisationUnit());
        var promotion2 = new Promotion();
        promotion2.setInstitution(new OrganisationUnit());
        var promotions = List.of(promotion1, promotion2);
        var promotionPage = new PageImpl<>(promotions, pageable, 2);

        when(promotionRepository.findAll(any(), anyBoolean(), eq(pageable))).thenReturn(
            promotionPage);

        // When
        var result = service.getAllPromotions(institutionId, nonFinishedOnly, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        if (institutionId < 1) {
            verify(promotionRepository).findAll(isNull(), anyBoolean(), eq(pageable));
        } else {
            verify(promotionRepository).findAll(anyInt(), anyBoolean(), eq(pageable));
        }
    }
}
