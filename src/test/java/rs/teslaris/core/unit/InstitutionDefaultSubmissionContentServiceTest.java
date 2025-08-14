package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.institution.InstitutionDefaultSubmissionContent;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.user.Authority;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.institution.InstitutionDefaultSubmissionContentRepository;
import rs.teslaris.core.service.impl.person.InstitutionDefaultSubmissionContentServiceImpl;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.user.UserService;

@SpringBootTest
class InstitutionDefaultSubmissionContentServiceTest {

    @Mock
    private InstitutionDefaultSubmissionContentRepository repository;

    @Mock
    private UserService userService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private InstitutionDefaultSubmissionContentServiceImpl service;


    @Test
    void shouldReturnDefaultContentForInstitution() {
        // Given
        Integer institutionId = 1;
        var hierarchy = List.of(2, 3);
        var defaultContent = new InstitutionDefaultSubmissionContent();
        defaultContent.setTypeOfTitle(Set.of());
        defaultContent.setPlaceOfKeep(Set.of());

        when(organisationUnitService.getSuperOUsHierarchyRecursive(institutionId)).thenReturn(
            hierarchy);
        when(repository.getDefaultContentForInstitution(1)).thenReturn(Optional.empty());
        when(repository.getDefaultContentForInstitution(2)).thenReturn(Optional.of(defaultContent));

        // When
        var result = service.readInstitutionDefaultContent(institutionId);

        // Then
        assertNotNull(result);
    }

    @Test
    void shouldReturnEmptyDTOWhenNoDefaultContentForInstitution() {
        // Given
        Integer institutionId = 1;
        when(organisationUnitService.getSuperOUsHierarchyRecursive(institutionId)).thenReturn(
            List.of());
        when(repository.getDefaultContentForInstitution(1)).thenReturn(Optional.empty());

        // When
        var result = service.readInstitutionDefaultContent(institutionId);

        // Then
        assertNotNull(result);
        assertTrue(result.typeOfTitle().isEmpty());
        assertTrue(result.placeOfKeep().isEmpty());
    }

    @Test
    void shouldReturnContentForInstitutionalEditorUser() {
        // Given
        Integer userId = 1;
        var orgUnit = new OrganisationUnit();
        orgUnit.setId(5);

        var user = new User();
        user.setAuthority(new Authority(UserRole.INSTITUTIONAL_EDITOR.name(), Set.of()));
        user.setOrganisationUnit(orgUnit);

        var hierarchy = List.of(6);
        var defaultContent = new InstitutionDefaultSubmissionContent();
        defaultContent.setTypeOfTitle(Set.of());
        defaultContent.setPlaceOfKeep(Set.of());

        when(userService.findOne(userId)).thenReturn(user);
        when(organisationUnitService.getSuperOUsHierarchyRecursive(5)).thenReturn(hierarchy);
        when(repository.getDefaultContentForInstitution(5)).thenReturn(Optional.empty());
        when(repository.getDefaultContentForInstitution(6)).thenReturn(Optional.of(defaultContent));

        // When
        var result = service.readInstitutionDefaultContentForUser(userId);

        // Then
        assertNotNull(result);
    }

    @Test
    void shouldReturnEmptyDTOForUnsupportedUserRole() {
        // Given
        Integer userId = 1;
        var user = new User();
        user.setAuthority(new Authority("SOME_OTHER_ROLE", Set.of()));

        when(userService.findOne(userId)).thenReturn(user);

        // When
        var result = service.readInstitutionDefaultContentForUser(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.typeOfTitle().isEmpty());
        assertTrue(result.placeOfKeep().isEmpty());
    }
}
