package rs.teslaris.thesislibrary.converter;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.person.ContactConverter;
import rs.teslaris.core.converter.person.PersonNameConverter;
import rs.teslaris.thesislibrary.dto.DissertationInformationDTO;
import rs.teslaris.thesislibrary.dto.PreviousTitleInformationDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookContactInformationDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookEntryDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookPersonalInformationDTO;
import rs.teslaris.thesislibrary.model.RegistryBookEntry;

public class RegistryBookEntryConverter {

    public static RegistryBookEntryDTO toDTO(RegistryBookEntry registryBookEntry) {
        var dto = new RegistryBookEntryDTO();
        dto.setId(registryBookEntry.getId());
        dto.setInPromotion(Objects.nonNull(registryBookEntry.getPromotion()));

        var dissertationInfoDTO = getDissertationInfoDTO(registryBookEntry);
        dto.setDissertationInformation(dissertationInfoDTO);

        var authorInfo = registryBookEntry.getPersonalInformation();
        var personalInfoDTO = new RegistryBookPersonalInformationDTO();

        if (Objects.nonNull(authorInfo.getAuthorName())) {
            personalInfoDTO.setAuthorName(PersonNameConverter.toDTO(authorInfo.getAuthorName()));
        }

        personalInfoDTO.setLocalBirthDate(authorInfo.getLocalBirthDate());
        personalInfoDTO.setPlaceOfBrith(authorInfo.getPlaceOfBrith());
        personalInfoDTO.setMunicipalityOfBrith(authorInfo.getMunicipalityOfBrith());

        if (Objects.nonNull(authorInfo.getCountryOfBirth())) {
            personalInfoDTO.setCountryOfBirthId(authorInfo.getCountryOfBirth().getId());
        }

        personalInfoDTO.setFatherName(authorInfo.getFatherName());
        personalInfoDTO.setFatherSurname(authorInfo.getFatherSurname());
        personalInfoDTO.setMotherName(authorInfo.getMotherName());
        personalInfoDTO.setMotherSurname(authorInfo.getMotherSurname());
        personalInfoDTO.setGuardianNameAndSurname(authorInfo.getGuardianNameAndSurname());
        dto.setPersonalInformation(personalInfoDTO);

        var contactInfo = registryBookEntry.getContactInformation();
        var contactInfoDTO = new RegistryBookContactInformationDTO();

        if (Objects.nonNull(contactInfo.getResidenceCountry())) {
            contactInfoDTO.setResidenceCountryId(contactInfo.getResidenceCountry().getId());
        }

        contactInfoDTO.setStreetAndNumber(contactInfo.getStreetAndNumber());
        contactInfoDTO.setPlace(contactInfo.getPlace());
        contactInfoDTO.setMunicipality(contactInfo.getMunicipality());
        contactInfoDTO.setPostalCode(contactInfo.getPostalCode());
        contactInfoDTO.setContact(ContactConverter.toDTO(contactInfo.getContact()));
        dto.setContactInformation(contactInfoDTO);

        var prevTitle = registryBookEntry.getPreviousTitleInformation();
        var prevTitleDTO = new PreviousTitleInformationDTO();
        prevTitleDTO.setInstitutionName(prevTitle.getInstitutionName());
        prevTitleDTO.setGraduationDate(prevTitle.getGraduationDate());
        prevTitleDTO.setInstitutionPlace(prevTitle.getInstitutionPlace());
        prevTitleDTO.setSchoolYear(prevTitle.getSchoolYear());
        dto.setPreviousTitleInformation(prevTitleDTO);

        return dto;
    }

    @NotNull
    private static DissertationInformationDTO getDissertationInfoDTO(
        RegistryBookEntry registryBookEntry) {
        var dissertationInfo = registryBookEntry.getDissertationInformation();
        var dissertationInfoDTO = new DissertationInformationDTO();
        dissertationInfoDTO.setDissertationTitle(dissertationInfo.getDissertationTitle());

        if (Objects.nonNull(dissertationInfo.getOrganisationUnit())) {
            dissertationInfoDTO.setOrganisationUnitId(
                dissertationInfo.getOrganisationUnit().getId());
            dissertationInfoDTO.setInstitutionName(
                MultilingualContentConverter.getMultilingualContentDTO(
                    dissertationInfo.getOrganisationUnit().getName()));
        }

        dissertationInfoDTO.setMentor(dissertationInfo.getMentor());
        dissertationInfoDTO.setCommission(dissertationInfo.getCommission());
        dissertationInfoDTO.setGrade(dissertationInfo.getGrade());
        dissertationInfoDTO.setAcquiredTitle(dissertationInfo.getAcquiredTitle());
        dissertationInfoDTO.setDefenceDate(dissertationInfo.getDefenceDate());
        dissertationInfoDTO.setDiplomaNumber(dissertationInfo.getDiplomaNumber());
        dissertationInfoDTO.setDiplomaIssueDate(dissertationInfo.getDiplomaIssueDate());
        dissertationInfoDTO.setDiplomaSupplementsNumber(
            dissertationInfo.getDiplomaSupplementsNumber());
        dissertationInfoDTO.setDiplomaSupplementsIssueDate(
            dissertationInfo.getDiplomaSupplementsIssueDate());
        return dissertationInfoDTO;
    }
}
