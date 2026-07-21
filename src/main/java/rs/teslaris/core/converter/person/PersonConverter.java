package rs.teslaris.core.converter.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.ExpertiseOrSkillResponseDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.dto.person.PersonUserResponseDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.dto.person.PrizeResponseDTO;
import rs.teslaris.core.dto.user.UserResponseDTO;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.ExpertiseOrSkill;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonFieldVisibility;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PersonNameType;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.model.person.Prize;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.repository.person.PersonFieldVisibilityRepository;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.core.util.session.SessionUtil;

@Component
public class PersonConverter {

    private static InvolvementRepository involvementRepository;

    private static PersonFieldVisibilityRepository personFieldVisibilityRepository;


    public PersonConverter(InvolvementRepository involvementRepository,
                           PersonFieldVisibilityRepository personFieldVisibilityRepository) {
        PersonConverter.involvementRepository = involvementRepository;
        PersonConverter.personFieldVisibilityRepository = personFieldVisibilityRepository;
    }

    public static PersonResponseDTO toDTO(Person person) {
        var otherNames = getPersonOtherNamesDTO(person.getOtherNames());

        var employmentIds = new ArrayList<Integer>();
        var educationIds = new ArrayList<Integer>();
        var membershipIds = new ArrayList<Integer>();
        setPersonInvolvementIds(person, employmentIds, educationIds, membershipIds);

        var expertisesOrSkills = new ArrayList<ExpertiseOrSkillResponseDTO>();
        setExpertisesAndSkills(person, expertisesOrSkills);

        var prizes = new ArrayList<PrizeResponseDTO>();
        person.getPrizes().stream()
            .sorted(Comparator.comparing(Prize::getId))
            .forEach(prize -> prizes.add(PrizeConverter.toDTO(prize)));

        var personResponse = new PersonResponseDTO(
            person.getId(),
            new PersonNameDTO(person.getName().getId(), person.getName().getFirstname(),
                person.getName().getOtherName(),
                person.getName().getLastname(), person.getName().getDateFrom(),
                person.getName().getDateTo(), person.getName().getNameType()),
            otherNames,
            toPersonalInfoDTO(person),
            MultilingualContentConverter.getMultilingualContentDTO(person.getBiography()),
            MultilingualContentConverter.getMultilingualContentDTO(person.getKeyword()),
            person.getApproveStatus(),
            employmentIds, educationIds, membershipIds,
            expertisesOrSkills, prizes,
            Objects.nonNull(person.getProfilePhoto()) ?
                person.getProfilePhoto().getImageServerName() : null,
            false
        );

        filterSensitiveData(personResponse, person);

        return personResponse;
    }

    public static PersonalInfoDTO toPersonalInfoDTO(Person person) {
        var professionalPostalAddress =
            getPostalAddressDTO(person.getPersonalInfo().getProfessionalPostalAddress());

        var privatePostalAddress =
            getPostalAddressDTO(person.getPersonalInfo().getPrivatePostalAddress());

        return new PersonalInfoDTO(
            person.getPersonalInfo().getLocalBirthDate(),
            person.getPersonalInfo().getPlaceOfBrith(),
            person.getPersonalInfo().getSex(),
            professionalPostalAddress,
            privatePostalAddress,
            toContactDTO(person.getPersonalInfo().getProfessionalContact()),
            toContactDTO(person.getPersonalInfo().getPrivateContact()),
            person.getApvnt(),
            person.getECrisId(),
            person.getENaukaId(),
            person.getOrcid(),
            person.getScopusAuthorId(),
            person.getOpenAlexId(),
            person.getWebOfScienceResearcherId(),
            person.getNationalScienceId(),
            person.getScholarId(),
            person.getAuthenticusId(),
            person.getLattesId(),
            person.getPersonalInfo().getUris(),
            MultilingualContentConverter.getMultilingualContentDTO(
                person.getPersonalInfo().getDisplayTitle()
            ),
            person.getId()
        );
    }

    private static ContactDTO toContactDTO(Contact contact) {
        if (Objects.isNull(contact)) {
            return new ContactDTO(null, null, null, null);
        }

        return new ContactDTO(
            contact.getContactEmail(),
            contact.getPhoneNumber(),
            contact.getFaxNumber(),
            contact.getMobilePhoneNumber()
        );
    }

    private static PostalAddressDTO getPostalAddressDTO(PostalAddress postalAddress) {
        var postalAddressDto = new PostalAddressDTO();

        if (Objects.isNull(postalAddress)) {
            return postalAddressDto;
        }

        if (Objects.nonNull(postalAddress.getCountry())) {
            postalAddressDto.setCountryId(postalAddress.getCountry().getId());
        }

        postalAddressDto.setStreetAndNumber(MultilingualContentConverter.getMultilingualContentDTO(
            postalAddress.getStreetAndNumber()));
        postalAddressDto.setCity(
            MultilingualContentConverter.getMultilingualContentDTO(postalAddress.getCity()));
        postalAddressDto.setState(
            MultilingualContentConverter.getMultilingualContentDTO(postalAddress.getState()));
        postalAddressDto.setPostalNumber(postalAddress.getPostalNumber());

        return postalAddressDto;
    }

    private static ArrayList<PersonNameDTO> getPersonOtherNamesDTO(Set<PersonName> otherNames) {
        if (Objects.isNull(otherNames)) {
            return new ArrayList<>();
        }

        return otherNames.stream()
            .sorted(Comparator.comparing(
                PersonName::getId,
                Comparator.nullsFirst(Integer::compareTo)
            ))
            .map(otherName -> new PersonNameDTO(otherName.getId(), otherName.getFirstname(),
                otherName.getOtherName(), otherName.getLastname(), otherName.getDateFrom(),
                otherName.getDateTo(),
                Objects.requireNonNullElse(otherName.getNameType(), PersonNameType.DISPLAY_NAME)))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private static void setPersonInvolvementIds(Person person, ArrayList<Integer> employmentIds,
                                                ArrayList<Integer> educationIds,
                                                ArrayList<Integer> membershipIds) {
        for (var inv : involvementRepository.findIdsTypesAndDatesByPersonId(person.getId())) {
            switch (inv.getInvolvementType()) {
                case HIRED_BY, EMPLOYED_AT, CANDIDATE -> employmentIds.add(inv.getId());
                case MEMBER_OF -> membershipIds.add(inv.getId());
                default -> educationIds.add(inv.getId());
            }
        }
    }

    private static void setExpertisesAndSkills(Person person,
                                               ArrayList<ExpertiseOrSkillResponseDTO> expertisesOrSkills) {
        person.getExpertisesAndSkills().stream()
            .sorted(Comparator.comparing(ExpertiseOrSkill::getId))
            .forEach(expertiseOrSkill ->
                expertisesOrSkills.add(ExpertiseOrSkillConverter.toDTO(expertiseOrSkill))
            );
    }

    public static PersonUserResponseDTO toDTOWithUser(Person person) {
        var otherNames = getPersonOtherNamesDTO(person.getOtherNames());

        var professionalPostalAddress =
            getPostalAddressDTO(person.getPersonalInfo().getProfessionalPostalAddress());

        var privatePostalAddress =
            getPostalAddressDTO(person.getPersonalInfo().getPrivatePostalAddress());

        UserResponseDTO userDTO = null;
        if (Objects.nonNull(person.getUser())) {
            userDTO = UserConverter.toUserResponseDTO(person.getUser());
        }

        var professionalContact = getContactDTO(person.getPersonalInfo().getProfessionalContact());

        var privateContact = getContactDTO(person.getPersonalInfo().getPrivateContact());

        var instituion = new Pair<Integer, List<MultilingualContentDTO>>(null, null);
        person.getInvolvements().stream().filter(i ->
                Objects.nonNull(i.getOrganisationUnit()) &&
                    i.getOrganisationUnit().getIsClientInstitutionCris() &&
                    List.of(InvolvementType.EMPLOYED_AT, InvolvementType.HIRED_BY)
                        .contains(i.getInvolvementType()) && Objects.isNull(i.getDateTo())).findAny()
            .ifPresent(currentInvolvement -> {
                instituion.a = currentInvolvement.getOrganisationUnit().getId();
                instituion.b = MultilingualContentConverter.getMultilingualContentDTO(
                    currentInvolvement.getOrganisationUnit().getName());
            });

        return new PersonUserResponseDTO(
            person.getId(),
            new PersonNameDTO(person.getName().getId(), person.getName().getFirstname(),
                person.getName().getOtherName(),
                person.getName().getLastname(), person.getName().getDateFrom(),
                person.getName().getDateTo(),
                Objects.requireNonNullElse(person.getName().getNameType(),
                    PersonNameType.DISPLAY_NAME)), otherNames,
            new PersonalInfoDTO(person.getPersonalInfo()
                .getLocalBirthDate(), person.getPersonalInfo().getPlaceOfBrith(),
                person.getPersonalInfo().getSex(), professionalPostalAddress, privatePostalAddress,
                professionalContact, privateContact, person.getApvnt(),
                person.getECrisId(), person.getENaukaId(), person.getOrcid(),
                person.getScopusAuthorId(), person.getOpenAlexId(),
                person.getWebOfScienceResearcherId(), person.getNationalScienceId(),
                person.getScholarId(), person.getAuthenticusId(), person.getLattesId(),
                person.getPersonalInfo().getUris(),
                MultilingualContentConverter.getMultilingualContentDTO(
                    person.getPersonalInfo().getDisplayTitle()), person.getId()),
            MultilingualContentConverter.getMultilingualContentDTO(person.getBiography()),
            MultilingualContentConverter.getMultilingualContentDTO(person.getKeyword()),
            person.getApproveStatus(), userDTO, instituion.b, instituion.a
        );
    }

    private static @NotNull ContactDTO getContactDTO(Contact person) {
        var professionalContact = new ContactDTO();
        if (Objects.nonNull(person)) {
            professionalContact.setContactEmail(
                person.getContactEmail());
            professionalContact.setPhoneNumber(
                person.getPhoneNumber());
            professionalContact.setFaxNumber(
                person.getFaxNumber());
            professionalContact.setMobilePhoneNumber(
                person.getMobilePhoneNumber());
        }
        return professionalContact;
    }

    private static void filterSensitiveData(PersonResponseDTO personResponse, Person person) {
        var fieldVisibilityConfiguration =
            personFieldVisibilityRepository.getFieldVisibilityConfiguration(person.getId())
                .orElse(new PersonFieldVisibility());

        if (!SessionUtil.isUserLoggedIn()) {
            if (!fieldVisibilityConfiguration.getContactEmailVisible()) {
                personResponse.getPersonalInfo().getContact().setContactEmail("");
                personResponse.getPersonalInfo().getPrivateContact().setContactEmail("");
            }

            if (!fieldVisibilityConfiguration.getPhoneNumberVisible()) {
                personResponse.getPersonalInfo().getContact().setPhoneNumber("");
                personResponse.getPersonalInfo().getPrivateContact().setPhoneNumber("");

                personResponse.getPersonalInfo().getContact().setMobilePhoneNumber("");
                personResponse.getPersonalInfo().getPrivateContact().setMobilePhoneNumber("");
            }

            if (!fieldVisibilityConfiguration.getBirthplaceVisible()) {
                personResponse.getPersonalInfo().setPlaceOfBirth(null);
            }

            if (!fieldVisibilityConfiguration.getDateOfBirthVisible()) {
                personResponse.getPersonalInfo().setLocalBirthDate(null);
            }

            if (!fieldVisibilityConfiguration.getSexVisible()) {
                personResponse.getPersonalInfo().setSex(null);
            }

            if (!fieldVisibilityConfiguration.getBiographyVisible()) {
                personResponse.setBiography(new ArrayList<>());
            }

            personResponse.getPersonalInfo().getPostalAddress().setCountryId(null);
            personResponse.getPersonalInfo().getPostalAddress().setCity(new ArrayList<>());
            personResponse.getPersonalInfo().getPostalAddress()
                .setStreetAndNumber(new ArrayList<>());

            personResponse.getPersonalInfo().getPrivatePostalAddress().setCountryId(null);
            personResponse.getPersonalInfo().getPrivatePostalAddress().setCity(new ArrayList<>());
            personResponse.getPersonalInfo().getPrivatePostalAddress()
                .setStreetAndNumber(new ArrayList<>());
        } else if (!SessionUtil.isUserLoggedInAndAdmin()) {
            if (fieldVisibilityConfiguration.getDateOfBirthVisible()) {
                personResponse.setShowFullBirthdate(true);
                return;
            }

            var userId = SessionUtil.getLoggedInUser().getId();
            if (Objects.isNull(userId)) {
                userId = 0;
            }

            if ((Objects.isNull(person.getUser()) || !userId.equals(person.getUser().getId())) &&
                Objects.nonNull(personResponse.getPersonalInfo().getLocalBirthDate())) {
                personResponse.getPersonalInfo().setLocalBirthDate(
                    LocalDate.of(personResponse.getPersonalInfo().getLocalBirthDate().getYear(), 1,
                        1));
            }
        }
    }
}
