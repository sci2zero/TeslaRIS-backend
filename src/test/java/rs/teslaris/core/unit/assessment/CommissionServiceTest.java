package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import rs.teslaris.core.assessment.dto.CommissionDTO;
import rs.teslaris.core.assessment.model.Commission;
import rs.teslaris.core.assessment.repository.CommissionRepository;
import rs.teslaris.core.assessment.service.impl.CommissionServiceImpl;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.CommissionReferenceConstraintViolationException;


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
        commission2.setSuperComission(commission1);

        when(commissionRepository.findAll(any(Pageable.class))).thenReturn(
            new PageImpl<>(List.of(commission1, commission2)));

        var response =
            commissionService.readAllCommissions(PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(2, response.getSize());
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
            List.of(1, 2, 3), List.of(1, 2, 3), "rule", 1);
        var newCommission = new Commission();
        newCommission.setId(2);

        when(documentPublicationService.findDocumentById(anyInt())).thenReturn(new Dataset());
        when(personService.findPersonById(anyInt())).thenReturn(new Person());
        when(organisationUnitService.findOrganisationUnitById(anyInt())).thenReturn(
            new OrganisationUnit());
        when(commissionRepository.findById(1)).thenReturn(Optional.of(new Commission()));

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
            List.of(1, 2, 3), List.of(1, 2, 3), "rule", 1);
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
}