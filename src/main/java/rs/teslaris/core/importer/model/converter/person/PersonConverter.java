package rs.teslaris.core.importer.model.converter.person;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.importer.model.organisationunit.OrgUnit;
import rs.teslaris.core.importer.model.person.Person;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.importer.utility.RecordConverter;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Sex;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;

@Component
@RequiredArgsConstructor
public class PersonConverter implements RecordConverter<Person, BasicPersonDTO> {

    private final OrganisationUnitService organisationUnitService;


    public BasicPersonDTO toDTO(Person person) {
        var dto = new BasicPersonDTO();

        dto.setOldId(OAIPMHParseUtility.parseBISISID(person.getId()));

        var personName = new PersonNameDTO();
        personName.setFirstname(person.getPersonName().getFirstNames());
        personName.setLastname(person.getPersonName().getFamilyNames());
        // TODO: Are other names supported?

        if (Objects.nonNull(person.getGender())) {
            dto.setSex(person.getGender().trim().equalsIgnoreCase("f") ? Sex.FEMALE : Sex.MALE);
        }

        if (Objects.nonNull(person.getElectronicAddresses())) {
            OAIPMHParseUtility.parseElectronicAddresses(person.getElectronicAddresses(), dto);
        }

        dto.setScopusAuthorId(person.getScopusAuthorId());
        dto.setOrcid(person.getOrcid());
        dto.setPersonName(personName);

        return dto;
    }

    public Optional<EmploymentDTO> toPersonEmployment(OrgUnit affiliation) {
        var dto = new EmploymentDTO();
        dto.setInvolvementType(InvolvementType.EMPLOYED_AT);

        dto.setRole(new ArrayList<>());
        dto.setProofs(new ArrayList<>());
        dto.setAffiliationStatement(new ArrayList<>());

        var organisationUnit = organisationUnitService.findOrganisationUnitByOldId(
            OAIPMHParseUtility.parseBISISID(affiliation.getId()));

        if (Objects.isNull(organisationUnit)) {
            return Optional.empty();
        }

        dto.setOrganisationUnitId(organisationUnit.getId());

        return Optional.of(dto);
    }
}
