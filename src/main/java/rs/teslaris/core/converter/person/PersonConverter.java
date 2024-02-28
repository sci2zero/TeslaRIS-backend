package rs.teslaris.core.converter.person;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PostalAddress;

public class PersonConverter {

    public static PersonResponseDTO toDTO(Person person) {
        var otherNames = getPersonOtherNamesDTO(person.getOtherNames());
        var biography = getPersonBiographyDTO(person.getBiography());
        var keyword = getPersonKeywordDTO(person.getKeyword());

        var postalAddress = getPostalAddressDTO(person.getPersonalInfo().getPostalAddress());

        return new PersonResponseDTO(
            new PersonNameDTO(person.getName().getFirstname(), person.getName().getOtherName(),
                person.getName().getLastname(), person.getName().getDateFrom(),
                person.getName().getDateTo()), otherNames,
            new PersonalInfoDTO(person.getPersonalInfo()
                .getLocalBirthDate(), person.getPersonalInfo().getPlaceOfBrith(),
                person.getPersonalInfo()
                    .getSex(), postalAddress,
                new ContactDTO(person.getPersonalInfo().getContact().getContactEmail(),
                    person.getPersonalInfo().getContact().getPhoneNumber()), person.getApvnt(),
                person.getMnid(), person.getOrcid(), person.getScopusAuthorId()), biography,
            keyword,
            person.getApproveStatus());
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
            new PersonNameDTO(otherName.getFirstname(), otherName.getOtherName(),
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
}
