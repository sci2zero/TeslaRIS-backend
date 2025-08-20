package rs.teslaris.core.converter.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.util.tracing.SessionTrackingUtil;

public class PersonConverter {

    public static PersonResponseDTO toDTO(Person person) {
        var otherNames = getPersonOtherNamesDTO(person.getOtherNames());
        var biography = getPersonBiographyDTO(person.getBiography());
        var keyword = getPersonKeywordDTO(person.getKeyword());

        var postalAddress = getPostalAddressDTO(person.getPersonalInfo().getPostalAddress());

        var employmentIds = new ArrayList<Integer>();
        var educationIds = new ArrayList<Integer>();
        var membershipIds = new ArrayList<Integer>();
        setPersonInvolvementIds(person, employmentIds, educationIds, membershipIds);

        var expertisesOrSkills = new ArrayList<ExpertiseOrSkillResponseDTO>();
        setExpertisesAndSkills(person, expertisesOrSkills);

        var prizes = new ArrayList<PrizeResponseDTO>();
        setPrizes(person, prizes);

        var personResponse = new PersonResponseDTO(
            person.getId(),
            new PersonNameDTO(person.getName().getId(), person.getName().getFirstname(),
                person.getName().getOtherName(),
                person.getName().getLastname(), person.getName().getDateFrom(),
                person.getName().getDateTo()), otherNames,
            new PersonalInfoDTO(person.getPersonalInfo()
                .getLocalBirthDate(), person.getPersonalInfo().getPlaceOfBrith(),
                person.getPersonalInfo()
                    .getSex(), postalAddress,
                new ContactDTO(Objects.nonNull(person.getPersonalInfo().getContact()) ?
                    person.getPersonalInfo().getContact().getContactEmail() : null,
                    Objects.nonNull(person.getPersonalInfo().getContact()) ?
                        person.getPersonalInfo().getContact().getPhoneNumber() : null),
                person.getApvnt(),
                person.getECrisId(), person.getENaukaId(), person.getOrcid(),
                person.getScopusAuthorId(), person.getOpenAlexId(),
                person.getWebOfScienceResearcherId(),
                person.getPersonalInfo().getUris(),
                MultilingualContentConverter.getMultilingualContentDTO(
                    person.getPersonalInfo().getDisplayTitle())), biography,
            keyword, person.getApproveStatus(), employmentIds, educationIds, membershipIds,
            expertisesOrSkills, prizes, Objects.nonNull(person.getProfilePhoto()) ?
            person.getProfilePhoto().getImageServerName() : null);

        filterSensitiveData(personResponse);

        return personResponse;
    }

    private static PostalAddressDTO getPostalAddressDTO(PostalAddress postalAddress) {
        var postalAddressDto = new PostalAddressDTO();
        if (Objects.nonNull(postalAddress.getCountry())) {
            postalAddressDto.setCountryId(postalAddress.getCountry().getId());
        }

        var streetAndNumberContent = new ArrayList<MultilingualContentDTO>();
        var cityContent = new ArrayList<MultilingualContentDTO>();

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

        postalAddressDto.setStreetAndNumber(streetAndNumberContent);
        postalAddressDto.setCity(cityContent);
        return postalAddressDto;
    }

    private static ArrayList<PersonNameDTO> getPersonOtherNamesDTO(Set<PersonName> otherNames) {
        var otherNamesDTO = new ArrayList<PersonNameDTO>();

        otherNames.forEach(otherName -> otherNamesDTO.add(
            new PersonNameDTO(otherName.getId(), otherName.getFirstname(), otherName.getOtherName(),
                otherName.getLastname(), otherName.getDateFrom(), otherName.getDateTo())));

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
        person.getInvolvements().stream()
            .sorted(Comparator.comparing(
                involvement -> Optional.ofNullable(involvement.getDateFrom())
                    .orElse(LocalDate.now()),
                Comparator.reverseOrder()
            ))
            .forEach(involvement -> {
                switch (involvement.getInvolvementType()) {
                    case HIRED_BY:
                    case EMPLOYED_AT:
                    case CANDIDATE:
                        employmentIds.add(involvement.getId());
                        break;
                    case MEMBER_OF:
                        membershipIds.add(involvement.getId());
                        break;
                    default:
                        educationIds.add(involvement.getId());
                }
            });
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

    private static void setPrizes(Person person, ArrayList<PrizeResponseDTO> prizes) {
        person.getPrizes().forEach(prize -> {
            var dto = new PrizeResponseDTO();
            dto.setId(prize.getId());
            dto.setTitle(
                MultilingualContentConverter.getMultilingualContentDTO(prize.getTitle()));
            dto.setDescription(
                MultilingualContentConverter.getMultilingualContentDTO(prize.getDescription()));
            dto.setDate(prize.getDate());

            dto.setProofs(new ArrayList<>());
            prize.getProofs().forEach(proof -> {
                dto.getProofs().add(DocumentFileConverter.toDTO(proof));
            });
            prizes.add(dto);
        });
    }

    public static PersonUserResponseDTO toDTOWithUser(Person person) {
        var otherNames = getPersonOtherNamesDTO(person.getOtherNames());
        var biography = getPersonBiographyDTO(person.getBiography());
        var keyword = getPersonKeywordDTO(person.getKeyword());

        var postalAddress = getPostalAddressDTO(person.getPersonalInfo().getPostalAddress());

        UserResponseDTO userDTO = null;
        if (Objects.nonNull(person.getUser())) {
            userDTO = UserConverter.toUserResponseDTO(person.getUser());
        }

        var contact = new ContactDTO();
        if (Objects.nonNull(person.getPersonalInfo().getContact())) {
            contact.setContactEmail(person.getPersonalInfo().getContact().getContactEmail());
            contact.setPhoneNumber(person.getPersonalInfo().getContact().getPhoneNumber());
        }

        return new PersonUserResponseDTO(
            person.getId(),
            new PersonNameDTO(person.getName().getId(), person.getName().getFirstname(),
                person.getName().getOtherName(),
                person.getName().getLastname(), person.getName().getDateFrom(),
                person.getName().getDateTo()), otherNames,
            new PersonalInfoDTO(person.getPersonalInfo()
                .getLocalBirthDate(), person.getPersonalInfo().getPlaceOfBrith(),
                person.getPersonalInfo().getSex(), postalAddress, contact, person.getApvnt(),
                person.getECrisId(), person.getENaukaId(), person.getOrcid(),
                person.getScopusAuthorId(), person.getOpenAlexId(),
                person.getWebOfScienceResearcherId(),
                person.getPersonalInfo().getUris(),
                MultilingualContentConverter.getMultilingualContentDTO(
                    person.getPersonalInfo().getDisplayTitle())), biography,
            keyword, person.getApproveStatus(), userDTO);
    }

    private static void filterSensitiveData(PersonResponseDTO personResponse) {
        if (!SessionTrackingUtil.isUserLoggedIn()) {
            personResponse.getPersonalInfo().getContact().setPhoneNumber("");
            personResponse.getPersonalInfo().getContact().setContactEmail("");
            personResponse.getPersonalInfo().setPlaceOfBirth(null);
            personResponse.getPersonalInfo().setLocalBirthDate(null);
            personResponse.getPersonalInfo().getPostalAddress().setCountryId(null);
            personResponse.getPersonalInfo().getPostalAddress().setCity(new ArrayList<>());
            personResponse.getPersonalInfo().getPostalAddress()
                .setStreetAndNumber(new ArrayList<>());
        }
    }
}
