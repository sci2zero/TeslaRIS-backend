package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rs.teslaris.assessment.dto.CommissionDTO;
import rs.teslaris.assessment.service.impl.CommissionServiceImpl;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.CommissionReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.language.LanguageAbbreviations;


@SpringBootTest
public class CommissionServiceTest {

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private PersonService personService;

    @Mock
    private DocumentPublicationService documentPublicationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Commission systemDefaultCommission;

    @InjectMocks
    private CommissionServiceImpl commissionService;


    @Test
    void shouldReadAllCommissions() {
        var commission1 = new Commission();
        commission1.setId(1);
        commission1.setFormalDescriptionOfRule("rule1");

        var commission2 = new Commission();
        commission2.setId(2);
        commission2.setFormalDescriptionOfRule("rule2");

        when(commissionRepository.searchCommissions(eq("aaa"), eq(LanguageAbbreviations.SERBIAN),
            any(Pageable.class))).thenReturn(
            new PageImpl<>(List.of(commission1, commission2)));

        var response =
            commissionService.readAllCommissions(PageRequest.of(0, 10), "aaa",
                LanguageAbbreviations.SERBIAN, false, false);

        assertNotNull(response);
        assertEquals(2, response.getTotalElements());
    }

    @Test
    void shouldReadCommission() {
        var commissionId = 1;
        var commission = new Commission();
        commission.setFormalDescriptionOfRule("rule");
        commission.setDescription(
            Set.of(new MultiLingualContent(new LanguageTag(), "Description", 1)));
        when(commissionRepository.findById(commissionId))
            .thenReturn(Optional.of(commission));

        var result = commissionService.readCommissionById(commissionId);

        assertEquals("rule", result.formalDescriptionOfRule());
        verify(commissionRepository).findById(commissionId);
    }

    @Test
    void shouldCreateCommission() {
        var commissionDTO = new CommissionDTO(null, List.of(new MultilingualContentDTO()),
            List.of("source1", "source2"),
            LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), List.of(1, 2, 3),
            List.of(1, 2, 3), List.of(1, 2, 3), "load-mno", List.of("NATURAL"), false);
        var newCommission = new Commission();
        newCommission.setId(2);

        when(documentPublicationService.findDocumentById(anyInt())).thenReturn(new Dataset());
        when(personService.findPersonById(anyInt())).thenReturn(new Person());
        when(organisationUnitService.findOrganisationUnitById(anyInt())).thenReturn(
            new OrganisationUnit());

        when(commissionRepository.save(any(Commission.class)))
            .thenReturn(newCommission);

        var result = commissionService.createCommission(
            commissionDTO);

        assertNotNull(result);
        assertEquals(2, newCommission.getId());
        verify(commissionRepository).save(any(Commission.class));
    }

    @Test
    void shouldUpdateCommission() {
        var commissionId = 1;
        var commissionDTO = new CommissionDTO(null, List.of(new MultilingualContentDTO()),
            List.of("source1", "source2"),
            LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), List.of(1, 2, 3),
            List.of(1, 2, 3), List.of(1, 2, 3), "load-mno", List.of("TECHNICAL"), false);
        var existingCommission = new Commission();

        when(commissionRepository.findById(commissionId))
            .thenReturn(Optional.of(existingCommission));

        when(documentPublicationService.findDocumentById(anyInt())).thenReturn(new Dataset());
        when(personService.findPersonById(anyInt())).thenReturn(new Person());
        when(organisationUnitService.findOrganisationUnitById(anyInt())).thenReturn(
            new OrganisationUnit());

        commissionService.updateCommission(commissionId,
            commissionDTO);

        verify(commissionRepository, atMost(2)).findById(commissionId);
        verify(commissionRepository).save(existingCommission);
    }

    @Test
    void shouldDeleteCommission() {
        // Given
        var commissionId = 1;

        when(commissionRepository.isInUse(commissionId)).thenReturn(false);
        when(commissionRepository.findById(commissionId)).thenReturn(Optional.of(new Commission()));

        // When
        commissionService.deleteCommission(commissionId);

        // Then
        verify(commissionRepository).save(any());
    }

    @Test
    void shouldThrowExceptionWhenDeletingCommissionInUse() {
        // Given
        var commissionId = 1;

        when(commissionRepository.isInUse(commissionId)).thenReturn(true);

        // When
        assertThrows(CommissionReferenceConstraintViolationException.class, () ->
            commissionService.deleteCommission(commissionId));

        // Then (CommissionReferenceConstraintViolationException should be thrown)
    }

    @Test
    void shouldThrowExceptionWhenCommissionNotFound() {
        // Given
        var commissionId = 1;

        // Simulating that the commissionRepository does not find the commission
        when(commissionRepository.findOneWithRelations(commissionId)).thenReturn(Optional.empty());

        // When
        assertThrows(NotFoundException.class, () ->
            commissionService.findOneWithFetchedRelations(commissionId));

        // Then (NotFoundException should be thrown with the correct message)
    }

    @Test
    void shouldReturnInstitutionIdForCommission() {
        // Given
        var commissionId = 1;
        var expectedInstitutionId = 42;

        // Simulating that the userRepository returns the institution ID
        when(userRepository.findOUIdForCommission(commissionId)).thenReturn(expectedInstitutionId);

        // When
        var institutionId = commissionService.findInstitutionIdForCommission(commissionId);

        // Then (The returned institution ID should match the expected value)
        assertEquals(expectedInstitutionId, institutionId);
    }

    @Test
    void returnsSystemDefaultWhenUserIdIsNull() {
        when(commissionRepository.findCommissionByIsDefaultTrue()).thenReturn(
            Optional.of(systemDefaultCommission));

        var result = commissionService.getDefaultCommission(null);

        assertEquals(systemDefaultCommission, result);
    }

    @Test
    void returnsSystemDefaultWhenOrganisationUnitIdIsNull() {
        var userId = 123;
        when(userRepository.findOrganisationUnitIdForUser(userId)).thenReturn(null);
        when(commissionRepository.findCommissionByIsDefaultTrue()).thenReturn(
            Optional.of(systemDefaultCommission));

        var result = commissionService.getDefaultCommission(userId);
        assertEquals(systemDefaultCommission, result);
    }

    @Test
    void returnsFirstAvailableCommissionInHierarchy() {
        var userId = 123;
        var rootOU = 10;
        var parentOU = 20;

        var expectedCommission = new Commission();

        when(userRepository.findOrganisationUnitIdForUser(userId)).thenReturn(rootOU);
        when(organisationUnitService.getSuperOUsHierarchyRecursive(rootOU))
            .thenReturn(List.of(parentOU));

        when(userRepository.findUserCommissionForOrganisationUnit(rootOU))
            .thenReturn(Collections.emptyList());

        when(userRepository.findUserCommissionForOrganisationUnit(parentOU))
            .thenReturn(List.of(expectedCommission));

        var result = commissionService.getDefaultCommission(userId);
        assertEquals(expectedCommission, result);
    }

    @Test
    void returnsSystemDefaultWhenNoCommissionsInHierarchy() {
        Integer userId = 123;
        Integer rootOU = 10;
        Integer parentOU = 20;

        when(userRepository.findOrganisationUnitIdForUser(userId)).thenReturn(rootOU);
        when(organisationUnitService.getSuperOUsHierarchyRecursive(rootOU))
            .thenReturn(List.of(parentOU));
        when(commissionRepository.findCommissionByIsDefaultTrue()).thenReturn(
            Optional.of(systemDefaultCommission));
        when(userRepository.findUserCommissionForOrganisationUnit(any()))
            .thenReturn(Collections.emptyList());

        Commission result = commissionService.getDefaultCommission(userId);
        assertEquals(systemDefaultCommission, result);
    }
}
