package rs.teslaris.thesislibrary.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.thesislibrary.converter.RegistryBookEntryConverter;
import rs.teslaris.thesislibrary.dto.DissertationInformationDTO;
import rs.teslaris.thesislibrary.dto.PreviousTitleInformationDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookContactInformationDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookEntryDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookPersonalInformationDTO;
import rs.teslaris.thesislibrary.model.DissertationInformation;
import rs.teslaris.thesislibrary.model.PreviousTitleInformation;
import rs.teslaris.thesislibrary.model.RegistryBookContactInformation;
import rs.teslaris.thesislibrary.model.RegistryBookEntry;
import rs.teslaris.thesislibrary.model.RegistryBookPersonalInformation;
import rs.teslaris.thesislibrary.repository.RegistryBookEntryRepository;
import rs.teslaris.thesislibrary.service.interfaces.PromotionService;
import rs.teslaris.thesislibrary.service.interfaces.RegistryBookService;

@Service
@RequiredArgsConstructor
@Transactional
public class RegistryBookServiceImpl extends JPAServiceImpl<RegistryBookEntry>
    implements RegistryBookService {

    private final RegistryBookEntryRepository registryBookEntryRepository;

    private final MultilingualContentService multilingualContentService;

    private final OrganisationUnitService organisationUnitService;

    private final CountryService countryService;

    private final PromotionService promotionService;


    @Override
    protected JpaRepository<RegistryBookEntry, Integer> getEntityRepository() {
        return registryBookEntryRepository;
    }

    @Override
    public Page<RegistryBookEntryDTO> getRegistryBookEntriesForPromotion(Integer promotionId,
                                                                         Pageable pageable) {
        return registryBookEntryRepository.getBookEntriesForPromotion(promotionId, pageable)
            .map(RegistryBookEntryConverter::toDTO);
    }

    @Override
    public RegistryBookEntry createRegistryBookEntry(RegistryBookEntryDTO dto) {
        var newEntry = new RegistryBookEntry();
        setCommonFields(newEntry, dto);
        return save(newEntry);
    }

    @Override
    public void updateRegistryBookEntry(Integer registryBookEntryId,
                                        RegistryBookEntryDTO dto) {
        var entry = findOne(registryBookEntryId);
        setCommonFields(entry, dto);
        save(entry);
    }

    @Override
    public void deleteRegistryBookEntry(Integer registryBookEntryId) {
        var entry = findOne(registryBookEntryId);
        registryBookEntryRepository.delete(entry);
    }

    private void setCommonFields(RegistryBookEntry entry, RegistryBookEntryDTO dto) {
        entry.setDissertationInformation(
            toDissertationInformation(dto.getDissertationInformation()));
        entry.setPersonalInformation(toPersonalInformation(dto.getPersonalInformation()));
        entry.setContactInformation(toContactInformation(dto.getContactInformation()));
        entry.setPreviousTitleInformation(
            toPreviousTitleInformation(dto.getPreviousTitleInformation()));
        entry.setPromotion(promotionService.findOne(dto.getPromotionId()));
    }

    private DissertationInformation toDissertationInformation(DissertationInformationDTO dto) {
        var di = new DissertationInformation();
        di.setDissertationTitle(
            multilingualContentService.getMultilingualContent(dto.getDissertationTitle()));
        di.setOrganisationUnit(organisationUnitService.findOne(dto.getOrganisationUnitId()));
        di.setMentor(dto.getMentor());
        di.setCommission(dto.getCommission());
        di.setGrade(dto.getGrade());
        di.setAcquiredTitle(dto.getAcquiredTitle());
        di.setDefenceDate(dto.getDefenceDate());
        di.setDiplomaNumber(dto.getDiplomaNumber());
        di.setDiplomaIssueDate(dto.getDiplomaIssueDate());
        di.setDiplomaSupplementsNumber(dto.getDiplomaSupplementsNumber());
        di.setDiplomaSupplementsIssueDate(dto.getDiplomaSupplementsIssueDate());
        return di;
    }

    private RegistryBookPersonalInformation toPersonalInformation(
        RegistryBookPersonalInformationDTO dto) {
        var pi = new RegistryBookPersonalInformation();
        pi.setAuthorName(
            new PersonName(dto.getAuthorName().getFirstname(), dto.getAuthorName().getOtherName(),
                dto.getAuthorName().getLastname(), null, null));
        pi.setLocalBirthDate(dto.getLocalBirthDate());
        pi.setPlaceOfBrith(dto.getPlaceOfBrith());
        pi.setMunicipalityOfBrith(dto.getMunicipalityOfBrith());
        pi.setCountryOfBirth(countryService.findOne(dto.getCountryOfBirthId()));
        pi.setFatherName(dto.getFatherName());
        pi.setFatherSurname(dto.getFatherSurname());
        pi.setMotherName(dto.getMotherName());
        pi.setMotherSurname(dto.getMotherSurname());
        pi.setGuardianNameAndSurname(dto.getGuardianNameAndSurname());
        return pi;
    }

    private RegistryBookContactInformation toContactInformation(
        RegistryBookContactInformationDTO dto) {
        var ci = new RegistryBookContactInformation();
        ci.setResidenceCountry(countryService.findOne(dto.getResidenceCountryId()));
        ci.setStreetAndNumber(dto.getStreetAndNumber());
        ci.setPlace(dto.getPlace());
        ci.setMunicipality(dto.getMunicipality());
        ci.setPostalCode(dto.getPostalCode());
        ci.setContact(
            new Contact(dto.getContact().getContactEmail(), dto.getContact().getPhoneNumber()));
        return ci;
    }

    private PreviousTitleInformation toPreviousTitleInformation(PreviousTitleInformationDTO dto) {
        var pti = new PreviousTitleInformation();
        pti.setInstitutionName(dto.getInstitutionName());
        pti.setGraduationDate(dto.getGraduationDate());
        pti.setInstitutionPlace(dto.getInstitutionPlace());
        pti.setSchoolYear(dto.getSchoolYear());
        return pti;
    }
}
