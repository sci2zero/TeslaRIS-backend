package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.ImportPersonDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.migrator.model.MigrationRun;

/**
 * Reads and updates a migrated person, which the core services offer in no single method.
 * <p>
 * The migration produces a flat {@link ImportPersonDTO}, while the person is read back as a
 * {@link PersonResponseDTO} that holds its personal information in a nested DTO. This writer
 * therefore projects the one shape onto the other, and it applies the merged DTO through the
 * granular person services. Each service is called only where the merge changed something, so an
 * unchanged person costs no write.
 */
@Component
@RequiredArgsConstructor
public class PersonMigrationWriter {

    private final PersonService personService;


    public ImportPersonDTO read(Integer personId) {
        var person = personService.readPersonWithBasicInfo(personId);
        var dto = new ImportPersonDTO();

        dto.setId(person.getId());
        dto.setPersonName(person.getPersonName());
        dto.setOtherNames(names(person.getPersonOtherNames()));
        dto.setBiography(content(person.getBiography()));
        dto.setKeywords(content(person.getKeyword()));

        var personalInfo = person.getPersonalInfo();

        if (Objects.isNull(personalInfo)) {
            return dto;
        }

        dto.setPlaceOfBirth(personalInfo.getPlaceOfBirth());
        dto.setLocalBirthDate(personalInfo.getLocalBirthDate());
        dto.setSex(personalInfo.getSex());
        dto.setOrcid(personalInfo.getOrcid());
        dto.setScopusAuthorId(personalInfo.getScopusAuthorId());
        dto.setOpenAlexId(personalInfo.getOpenAlexId());
        dto.setWebOfScienceResearcherId(personalInfo.getWebOfScienceResearcherId());
        dto.setNationalScienceId(personalInfo.getNationalScienceId());
        dto.setScholarId(personalInfo.getScholarId());
        dto.setAuthenticusId(personalInfo.getAuthenticusId());
        dto.setLattesId(personalInfo.getLattesId());
        dto.setUris(uris(personalInfo.getUris()));
        dto.setDisplayTitle(content(personalInfo.getDisplayTitle()));
        dto.setResearchAreasId(researchAreas(personalInfo.getResearchAreasId()));

        var address = personalInfo.getPostalAddress();

        if (Objects.nonNull(address)) {
            dto.setAddressLine(content(address.getStreetAndNumber()));
            dto.setAddressCity(content(address.getCity()));
            dto.setAddressState(content(address.getState()));
            dto.setPostalNumber(address.getPostalNumber());
        }

        var privateAddress = personalInfo.getPrivatePostalAddress();

        if (Objects.nonNull(privateAddress)) {
            dto.setPrivateAddressLine(content(privateAddress.getStreetAndNumber()));
            dto.setPrivateAddressCity(content(privateAddress.getCity()));
            dto.setPrivateAddressState(content(privateAddress.getState()));
            dto.setPrivatePostalNumber(privateAddress.getPostalNumber());
        }

        var contact = personalInfo.getContact();

        if (Objects.nonNull(contact)) {
            dto.setContactEmail(contact.getContactEmail());
            dto.setPhoneNumber(contact.getPhoneNumber());
            dto.setFaxNumber(contact.getFaxNumber());
            dto.setMobilePhoneNumber(contact.getMobilePhoneNumber());
        }

        var privateContact = personalInfo.getPrivateContact();

        if (Objects.nonNull(privateContact)) {
            dto.setPrivateContactEmail(privateContact.getContactEmail());
            dto.setPrivatePhoneNumber(privateContact.getPhoneNumber());
            dto.setPrivateFaxNumber(privateContact.getFaxNumber());
            dto.setPrivateMobilePhoneNumber(privateContact.getMobilePhoneNumber());
        }

        return dto;
    }

    public void update(Integer personId, ImportPersonDTO merged, MigrationRun run) {
        var person = personService.readPersonWithBasicInfo(personId);

        if (Objects.nonNull(merged.getPersonName()) &&
            nameChanged(person.getPersonName(), merged.getPersonName())) {
            personService.updatePersonMainName(personId, merged.getPersonName());
        }

        if (namesChanged(person.getPersonOtherNames(), merged.getOtherNames())) {
            personService.setPersonOtherNames(merged.getOtherNames(), personId);
        }

        if (contentChanged(person.getBiography(), merged.getBiography())) {
            personService.setPersonBiography(merged.getBiography(), personId);
        }

        if (contentChanged(person.getKeyword(), merged.getKeywords())) {
            personService.setPersonKeyword(merged.getKeywords(), personId);
        }

        var personalInfo = Objects.isNull(person.getPersonalInfo())
            ? new PersonalInfoDTO()
            : new PersonalInfoDTO(person.getPersonalInfo());

        if (applyPersonalInfo(personalInfo, merged)) {
            personService.updatePersonalInfo(personId, personalInfo);
        }
    }

    private boolean applyPersonalInfo(PersonalInfoDTO target, ImportPersonDTO merged) {
        var changed = false;

        changed |= set(merged.getPlaceOfBirth(), target::getPlaceOfBirth, target::setPlaceOfBirth);
        changed |=
            set(merged.getLocalBirthDate(), target::getLocalBirthDate, target::setLocalBirthDate);
        changed |= set(merged.getSex(), target::getSex, target::setSex);
        changed |= set(merged.getOrcid(), target::getOrcid, target::setOrcid);
        changed |= set(merged.getScopusAuthorId(), target::getScopusAuthorId,
            target::setScopusAuthorId);
        changed |= set(merged.getOpenAlexId(), target::getOpenAlexId, target::setOpenAlexId);
        changed |= set(merged.getWebOfScienceResearcherId(), target::getWebOfScienceResearcherId,
            target::setWebOfScienceResearcherId);
        changed |= set(merged.getNationalScienceId(), target::getNationalScienceId,
            target::setNationalScienceId);
        changed |= set(merged.getScholarId(), target::getScholarId, target::setScholarId);
        changed |= set(merged.getAuthenticusId(), target::getAuthenticusId,
            target::setAuthenticusId);
        changed |= set(merged.getLattesId(), target::getLattesId, target::setLattesId);
        changed |= set(merged.getUris(), target::getUris, target::setUris);
        changed |= setContent(merged.getDisplayTitle(), target::getDisplayTitle,
            target::setDisplayTitle);
        changed |= set(merged.getResearchAreasId(), target::getResearchAreasId,
            target::setResearchAreasId);

        if (Objects.isNull(target.getPostalAddress())) {
            target.setPostalAddress(new PostalAddressDTO());
        }

        var address = target.getPostalAddress();
        changed |= setContent(merged.getAddressLine(), address::getStreetAndNumber,
            address::setStreetAndNumber);
        changed |= setContent(merged.getAddressCity(), address::getCity, address::setCity);
        changed |= setContent(merged.getAddressState(), address::getState, address::setState);
        changed |= set(merged.getPostalNumber(), address::getPostalNumber,
            address::setPostalNumber);

        if (Objects.isNull(target.getPrivatePostalAddress())) {
            target.setPrivatePostalAddress(new PostalAddressDTO());
        }

        var privateAddress = target.getPrivatePostalAddress();
        changed |= setContent(merged.getPrivateAddressLine(), privateAddress::getStreetAndNumber,
            privateAddress::setStreetAndNumber);
        changed |=
            setContent(merged.getPrivateAddressCity(), privateAddress::getCity,
                privateAddress::setCity);
        changed |= setContent(merged.getPrivateAddressState(), privateAddress::getState,
            privateAddress::setState);
        changed |= set(merged.getPrivatePostalNumber(), privateAddress::getPostalNumber,
            privateAddress::setPostalNumber);

        if (Objects.isNull(target.getContact())) {
            target.setContact(new ContactDTO());
        }

        var contact = target.getContact();
        changed |= set(merged.getContactEmail(), contact::getContactEmail,
            contact::setContactEmail);
        changed |= set(merged.getPhoneNumber(), contact::getPhoneNumber, contact::setPhoneNumber);
        changed |= set(merged.getFaxNumber(), contact::getFaxNumber, contact::setFaxNumber);
        changed |= set(merged.getMobilePhoneNumber(), contact::getMobilePhoneNumber,
            contact::setMobilePhoneNumber);

        if (Objects.isNull(target.getPrivateContact())) {
            target.setPrivateContact(new ContactDTO());
        }

        var privateContact = target.getPrivateContact();
        changed |= set(merged.getPrivateContactEmail(), privateContact::getContactEmail,
            privateContact::setContactEmail);
        changed |= set(merged.getPrivatePhoneNumber(), privateContact::getPhoneNumber,
            privateContact::setPhoneNumber);
        changed |= set(merged.getPrivateFaxNumber(), privateContact::getFaxNumber,
            privateContact::setFaxNumber);
        changed |= set(merged.getPrivateMobilePhoneNumber(), privateContact::getMobilePhoneNumber,
            privateContact::setMobilePhoneNumber);

        return changed;
    }

    private <T> boolean set(T value, Supplier<T> getter, Consumer<T> setter) {
        if (Objects.isNull(value) || Objects.equals(value, getter.get())) {
            return false;
        }

        setter.accept(value);

        return true;
    }

    private boolean setContent(List<MultilingualContentDTO> value,
                               Supplier<List<MultilingualContentDTO>> getter,
                               Consumer<List<MultilingualContentDTO>> setter) {
        if (Objects.isNull(value) || !contentChanged(getter.get(), value)) {
            return false;
        }

        setter.accept(value);

        return true;
    }

    private boolean contentChanged(List<MultilingualContentDTO> current,
                                   List<MultilingualContentDTO> candidate) {
        if (Objects.isNull(candidate)) {
            return false;
        }

        return !contentValues(current).equals(contentValues(candidate));
    }

    private Set<String> contentValues(List<MultilingualContentDTO> content) {
        if (Objects.isNull(content)) {
            return Set.of();
        }

        return content.stream()
            .filter(Objects::nonNull)
            .map(entry -> entry.getLanguageTag() + "=" + entry.getContent())
            .collect(Collectors.toSet());
    }

    private boolean nameChanged(PersonNameDTO current, PersonNameDTO candidate) {
        if (Objects.isNull(current)) {
            return true;
        }

        return !Objects.equals(current.getFirstname(), candidate.getFirstname()) ||
            !Objects.equals(current.getOtherName(), candidate.getOtherName()) ||
            !Objects.equals(current.getLastname(), candidate.getLastname());
    }

    private boolean namesChanged(List<PersonNameDTO> current, List<PersonNameDTO> candidate) {
        if (Objects.isNull(candidate) || candidate.isEmpty()) {
            return false;
        }

        return !nameValues(current).equals(nameValues(candidate));
    }

    private Set<String> nameValues(List<PersonNameDTO> names) {
        if (Objects.isNull(names)) {
            return Set.of();
        }

        return names.stream()
            .filter(Objects::nonNull)
            .map(name -> name.getFirstname() + " " + name.getOtherName() + " " +
                name.getLastname())
            .collect(Collectors.toSet());
    }

    private List<MultilingualContentDTO> content(List<MultilingualContentDTO> content) {
        return Objects.isNull(content) ? new ArrayList<>() : new ArrayList<>(content);
    }

    private List<PersonNameDTO> names(List<PersonNameDTO> names) {
        return Objects.isNull(names) ? new ArrayList<>() : new ArrayList<>(names);
    }

    private Set<String> uris(Set<String> uris) {
        return Objects.isNull(uris) ? new HashSet<>() : new HashSet<>(uris);
    }

    private List<Integer> researchAreas(List<Integer> researchAreasId) {
        return Objects.isNull(researchAreasId) ? new ArrayList<>() :
            new ArrayList<>(researchAreasId);
    }
}
