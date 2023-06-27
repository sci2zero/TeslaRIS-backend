package rs.teslaris.core;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.CountryService;
import rs.teslaris.core.service.MultilingualContentService;
import rs.teslaris.core.service.OrganisationUnitService;
import rs.teslaris.core.service.PersonService;
import rs.teslaris.core.service.impl.PersonContributionServiceImpl;

@SpringBootTest
public class PersonContributionServiceTest {

    @Mock
    private PersonService personService;

    @Mock
    private CountryService countryService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @InjectMocks
    private PersonContributionServiceImpl personContributionService;


    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(personContributionService, "contributionApprovedByDefault",
            true);
    }

    @Test
    public void testSetPersonDocumentContributionsForDocument() {
        // Given
        var document = new JournalPublication();
        document.setContributors(new HashSet<>());
        var documentDTO = new DocumentDTO();

        var contributionDTO1 = new PersonDocumentContributionDTO();
        contributionDTO1.setContributionType(DocumentContributionType.AUTHOR);
        contributionDTO1.setIsMainContributor(true);
        contributionDTO1.setIsCorrespondingContributor(false);
        contributionDTO1.setContributionDescription(new ArrayList<>());
        contributionDTO1.setOrderNumber(1);
        contributionDTO1.setPersonId(1);
        contributionDTO1.setPersonName(new PersonNameDTO());
        contributionDTO1.setContact(new ContactDTO());

        contributionDTO1.setPostalAddress(
            new PostalAddressDTO(null, new ArrayList<>(), new ArrayList<>()));
        contributionDTO1.setContact(new ContactDTO(null, "phone"));

        var institutionIds1 = new ArrayList<Integer>();
        institutionIds1.add(1);
        institutionIds1.add(2);
        contributionDTO1.setInstitutionIds(institutionIds1);

        var contributionDTO2 = new PersonDocumentContributionDTO();
        contributionDTO2.setContributionType(DocumentContributionType.AUTHOR);
        contributionDTO2.setIsMainContributor(false);
        contributionDTO2.setIsCorrespondingContributor(true);
        contributionDTO2.setContributionDescription(new ArrayList<>());
        contributionDTO2.setOrderNumber(2);
        contributionDTO2.setPersonName(new PersonNameDTO());
        contributionDTO2.setPostalAddress(
            new PostalAddressDTO(1, new ArrayList<>(), new ArrayList<>()));
        contributionDTO2.setContact(new ContactDTO("email", null));

        var institutionIds2 = new ArrayList<Integer>();
        institutionIds2.add(3);
        institutionIds2.add(4);
        contributionDTO2.setInstitutionIds(institutionIds2);

        var contributionsDTO = new ArrayList<PersonDocumentContributionDTO>();
        contributionsDTO.add(contributionDTO1);
        contributionsDTO.add(contributionDTO2);
        documentDTO.setContributions(contributionsDTO);

        when(personService.findPersonById(1)).thenReturn(new Person());
        when(countryService.findCountryById(1)).thenReturn(new Country());

        // When
        personContributionService.setPersonDocumentContributionsForDocument(document, documentDTO);

        // Then
        var contributions = document.getContributors();
        assertTrue(contributions.size() > 0);
    }
}
