package rs.teslaris.core.converter.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
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
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonFieldVisibility;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PersonNameType;
import rs.teslaris.core.model.person.PostalAddress;
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
        var biography = getPersonBiographyDTO(person.getBiography());
        var keyword = getPersonKeywordDTO(person.getKeyword());

        var professionalostalAddress =
            getPostalAddressDTO(person.getPersonalInfo().getProfessionalPostalAddress());

        var privatePostalAddress =
            getPostalAddressDTO(person.getPersonalInfo().getPrivatePostalAddress());

        var employmentIds = new ArrayList<Integer>();
        var educationIds = new ArrayList<Integer>();
        var membershipIds = new ArrayList<Integer>();
        setPersonInvolvementIds(person, employmentIds, educationIds, membershipIds);

        var expertisesOrSkills = new ArrayList<ExpertiseOrSkillResponseDTO>();
        setExpertisesAndSkills(person, expertisesOrSkills);

        var prizes = new ArrayList<PrizeResponseDTO>();
        person.getPrizes().forEach(prize -> prizes.add(PrizeConverter.toDTO(prize)));

        var personResponse = new PersonResponseDTO(
            person.getId(),
            new PersonNameDTO(person.getName().getId(), person.getName().getFirstname(),
                person.getName().getOtherName(),
                person.getName().getLastname(), person.getName().getDateFrom(),
                person.getName().getDateTo(), person.getName().getNameType()), otherNames,
            new PersonalInfoDTO(person.getPersonalInfo()
                .getLocalBirthDate(), person.getPersonalInfo().getPlaceOfBrith(),
                person.getPersonalInfo()
                    .getSex(), professionalostalAddress, privatePostalAddress,
                new ContactDTO(Objects.nonNull(person.getPersonalInfo().getProfessionalContact()) ?
                    person.getPersonalInfo().getProfessionalContact().getContactEmail() : null,
                    Objects.nonNull(person.getPersonalInfo().getProfessionalContact()) ?
                        person.getPersonalInfo().getProfessionalContact().getPhoneNumber() : null,
                    Objects.nonNull(person.getPersonalInfo().getProfessionalContact()) ?
                        person.getPersonalInfo().getProfessionalContact().getFaxNumber() : null,
                    Objects.nonNull(person.getPersonalInfo().getProfessionalContact()) ?
                        person.getPersonalInfo().getProfessionalContact().getMobilePhoneNumber() :
                        null),
                new ContactDTO(Objects.nonNull(person.getPersonalInfo().getPrivateContact()) ?
                    person.getPersonalInfo().getPrivateContact().getContactEmail() : null,
                    Objects.nonNull(person.getPersonalInfo().getPrivateContact()) ?
                        person.getPersonalInfo().getPrivateContact().getPhoneNumber() : null,
                    Objects.nonNull(person.getPersonalInfo().getPrivateContact()) ?
                        person.getPersonalInfo().getPrivateContact().getFaxNumber() : null,
                    Objects.nonNull(person.getPersonalInfo().getPrivateContact()) ?
                        person.getPersonalInfo().getPrivateContact().getMobilePhoneNumber() :
                        null),
                person.getApvnt(),
                person.getECrisId(), person.getENaukaId(), person.getOrcid(),
                person.getScopusAuthorId(), person.getOpenAlexId(),
                person.getWebOfScienceResearcherId(), person.getNationalScienceId(),
                person.getScholarId(), person.getAuthenticusId(), person.getLattesId(),
                person.getPersonalInfo().getUris(),
                MultilingualContentConverter.getMultilingualContentDTO(
                    person.getPersonalInfo().getDisplayTitle())), biography,
            keyword, person.getApproveStatus(), employmentIds, educationIds, membershipIds,
            expertisesOrSkills, prizes, Objects.nonNull(person.getProfilePhoto()) ?
            person.getProfilePhoto().getImageServerName() : null, false);

        filterSensitiveData(personResponse, person);

        return personResponse;
    }

    private static PostalAddressDTO getPostalAddressDTO(PostalAddress postalAddress) {
        var postalAddressDto = new PostalAddressDTO();

        if (Objects.isNull(postalAddress)) {
            return postalAddressDto;
        }

        if (Objects.nonNull(postalAddress.getCountry())) {
            postalAddressDto.setCountryId(postalAddress.getCountry().getId());
        }

        var streetAndNumberContent = new ArrayList<MultilingualContentDTO>();
        var cityContent = new ArrayList<MultilingualContentDTO>();
        var stateContent = new ArrayList<MultilingualContentDTO>();

        for (var streetAndNumber : postalAddress.getStreetAndNumber()) {
            streetAndNumberContent.add(
                new MultilingualContentDTO(streetAndNumber.getLanguage().getId(),
                    streetAndNumber.getLanguage().getLanguageTag(),
                    streetAndNumber.getContent(),
                    streetAndNumber.getPriority()));
        }

        for (var city : postalAddress.getCity()) {
            cityContent.add(
                new MultilingualContentDTO(city.getLanguage().getId(),
                    city.getLanguage().getLanguageTag(),
                    city.getContent(),
                    city.getPriority()));
        }

        for (var state : postalAddress.getState()) {
            stateContent.add(
                new MultilingualContentDTO(state.getLanguage().getId(),
                    state.getLanguage().getLanguageTag(),
                    state.getContent(),
                    state.getPriority()));
        }

        postalAddressDto.setStreetAndNumber(streetAndNumberContent);
        postalAddressDto.setCity(cityContent);
        postalAddressDto.setState(stateContent);
        postalAddressDto.setPostalNumber(postalAddress.getPostalNumber());

        return postalAddressDto;
    }

    private static ArrayList<PersonNameDTO> getPersonOtherNamesDTO(Set<PersonName> otherNames) {
        var otherNamesDTO = new ArrayList<PersonNameDTO>();

        otherNames.forEach(otherName -> otherNamesDTO.add(
            new PersonNameDTO(otherName.getId(), otherName.getFirstname(), otherName.getOtherName(),
                otherName.getLastname(), otherName.getDateFrom(), otherName.getDateTo(),
                Objects.requireNonNullElse(otherName.getNameType(), PersonNameType.DISPLAY_NAME))));

        return otherNamesDTO;
    }

    private static ArrayList<MultilingualContentDTO> getPersonBiographyDTO(
        Set<MultiLingualContent> biography) {
        var biographyDTO = new ArrayList<MultilingualContentDTO>();

        biography.forEach(bio -> biographyDTO.add(
            new MultilingualContentDTO(bio.getLanguage().getId(),
                bio.getLanguage().getLanguageTag(),
                bio.getContent(),
                bio.getPriority())));

        return biographyDTO;
    }

    private static ArrayList<MultilingualContentDTO> getPersonKeywordDTO(
        Set<MultiLingualContent> keyword) {
        var keywordDTO = new ArrayList<MultilingualContentDTO>();

        keyword.forEach(keyw -> keywordDTO.add(
            new MultilingualContentDTO(keyw.getLanguage().getId(),
                keyw.getLanguage().getLanguageTag(),
                keyw.getContent(),
                keyw.getPriority())));

        return keywordDTO;
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
        person.getExpertisesAndSkills().forEach(expertiseOrSkill -> {
            var dto = new ExpertiseOrSkillResponseDTO();
            dto.setId(expertiseOrSkill.getId());
            dto.setName(
                MultilingualContentConverter.getMultilingualContentDTO(expertiseOrSkill.getName()));
            dto.setDescription(MultilingualContentConverter.getMultilingualContentDTO(
                expertiseOrSkill.getDescription()));

            dto.setProofs(new ArrayList<>());
            expertiseOrSkill.getProofs().forEach(proof -> {
                dto.getProofs().add(DocumentFileConverter.toDTO(proof));
            });
            expertisesOrSkills.add(dto);
        });
    }

    public static PersonUserResponseDTO toDTOWithUser(Person person) {
        var otherNames = getPersonOtherNamesDTO(person.getOtherNames());
        var biography = getPersonBiographyDTO(person.getBiography());
        var keyword = getPersonKeywordDTO(person.getKeyword());

        var professionalPostalAddress =
            getPostalAddressDTO(person.getPersonalInfo().getProfessionalPostalAddress());

        var privatePostalAddress =
            getPostalAddressDTO(person.getPersonalInfo().getPrivatePostalAddress());

        UserResponseDTO userDTO = null;
        if (Objects.nonNull(person.getUser())) {
            userDTO = UserConverter.toUserResponseDTO(person.getUser());
        }

        var professionalContact = new ContactDTO();
        if (Objects.nonNull(person.getPersonalInfo().getProfessionalContact())) {
            professionalContact.setContactEmail(
                person.getPersonalInfo().getProfessionalContact().getContactEmail());
            professionalContact.setPhoneNumber(
                person.getPersonalInfo().getProfessionalContact().getPhoneNumber());
            professionalContact.setFaxNumber(
                person.getPersonalInfo().getProfessionalContact().getFaxNumber());
            professionalContact.setMobilePhoneNumber(
                person.getPersonalInfo().getProfessionalContact().getMobilePhoneNumber());
        }

        var privateContact = new ContactDTO();
        if (Objects.nonNull(person.getPersonalInfo().getPrivateContact())) {
            privateContact.setContactEmail(
                person.getPersonalInfo().getPrivateContact().getContactEmail());
            privateContact.setPhoneNumber(
                person.getPersonalInfo().getPrivateContact().getPhoneNumber());
            privateContact.setFaxNumber(
                person.getPersonalInfo().getPrivateContact().getFaxNumber());
            privateContact.setMobilePhoneNumber(
                person.getPersonalInfo().getPrivateContact().getMobilePhoneNumber());
        }

        var instituion = new Pair<Integer, List<MultilingualContentDTO>>(null, null);
        person.getInvolvements().stream().filter(i -> Objects.nonNull(i.getOrganisationUnit()) &&
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
                    person.getPersonalInfo().getDisplayTitle())), biography,
            keyword, person.getApproveStatus(), userDTO, instituion.b, instituion.a);
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
