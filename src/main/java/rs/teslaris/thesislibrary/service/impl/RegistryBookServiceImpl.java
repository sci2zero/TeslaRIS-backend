package rs.teslaris.thesislibrary.service.impl;

import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.person.PersonNameConverter;
import rs.teslaris.core.converter.person.PostalAddressConverter;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.repository.person.EmploymentRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.ThesisException;
import rs.teslaris.core.util.language.SerbianTransliteration;
import rs.teslaris.thesislibrary.converter.RegistryBookEntryConverter;
import rs.teslaris.thesislibrary.dto.DissertationInformationDTO;
import rs.teslaris.thesislibrary.dto.PhdThesisPrePopulatedDataDTO;
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

    private final ThesisService thesisService;

    private final EmploymentRepository employmentRepository;


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

        if (Objects.nonNull(dto.getOrganisationUnitId())) {
            di.setOrganisationUnit(organisationUnitService.findOne(dto.getOrganisationUnitId()));
        }

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

        if (Objects.nonNull(dto.getCountryOfBirthId())) {
            pi.setCountryOfBirth(countryService.findOne(dto.getCountryOfBirthId()));
        }

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

        if (Objects.nonNull(dto.getResidenceCountryId())) {
            ci.setResidenceCountry(countryService.findOne(dto.getResidenceCountryId()));
        }

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
        pti.setAcademicTitle(dto.getAcademicTitle());
        return pti;
    }

    @Override
    public PhdThesisPrePopulatedDataDTO getPrePopulatedPHDThesisInformation(Integer thesisId) {
        var phdThesis = thesisService.getThesisById(thesisId);

        if (phdThesis.getThesisDefenceDate() == null ||
            !(ThesisType.PHD.equals(phdThesis.getThesisType()) ||
                ThesisType.PHD_ART_PROJECT.equals(phdThesis.getThesisType()))) {
            throw new ThesisException(
                "This functionality is only available for defended PHD theses and PHD art projects.");
        }

        var prePopulatedData = new PhdThesisPrePopulatedDataDTO();

        populateAuthorInformation(phdThesis, prePopulatedData);
        populateMentorInformation(phdThesis, prePopulatedData);
        populateCommissionInformation(phdThesis, prePopulatedData);
        populateInstitutionInformation(phdThesis, prePopulatedData);

        prePopulatedData.setTitle(getTransliteratedContent(phdThesis.getTitle()));
        prePopulatedData.setDefenceDate(phdThesis.getThesisDefenceDate());

        return prePopulatedData;
    }

    private void populateAuthorInformation(Thesis phdThesis, PhdThesisPrePopulatedDataDTO dto) {
        phdThesis.getContributors().stream()
            .filter(c -> DocumentContributionType.AUTHOR.equals(c.getContributionType()))
            .findFirst()
            .ifPresent(author -> {
                dto.setPersonName(PersonNameConverter.toDTO(
                    author.getAffiliationStatement().getDisplayPersonName()));
                var person = author.getPerson();
                if (Objects.nonNull(person) && Objects.nonNull(person.getPersonalInfo())) {
                    var info = person.getPersonalInfo();
                    dto.setLocalBirthDate(info.getLocalBirthDate());
                    dto.setPlaceOfBirth(info.getPlaceOfBrith());
                    dto.setPostalAddress(PostalAddressConverter.toDto(info.getPostalAddress()));
                    dto.setContact(new ContactDTO(info.getContact().getContactEmail(),
                        info.getContact().getPhoneNumber()));
                }
            });
    }

    private void populateMentorInformation(Thesis phdThesis, PhdThesisPrePopulatedDataDTO dto) {
        phdThesis.getContributors().stream()
            .filter(c -> DocumentContributionType.ADVISOR.equals(c.getContributionType()))
            .findFirst()
            .ifPresent(mentor -> {
                StringBuilder sb = new StringBuilder();
                sb.append(mentor.getPersonalTitle().getValue()).append(" ")
                    .append(mentor.getAffiliationStatement().getDisplayPersonName())
                    .append(", ").append(mentor.getEmploymentTitle().getValue());

                mentor.getInstitutions().stream().findFirst()
                    .ifPresent(
                        inst -> sb.append(" ").append(getTransliteratedContent(inst.getName())));

                dto.setMentor(sb.toString().trim());
            });
    }

    private void populateCommissionInformation(Thesis phdThesis, PhdThesisPrePopulatedDataDTO dto) {
        StringBuilder sb = new StringBuilder();
        phdThesis.getContributors().stream()
            .filter(c -> DocumentContributionType.BOARD_MEMBER.equals(c.getContributionType()))
            .forEach(member -> {
                if (!sb.isEmpty()) {
                    sb.append("\n");
                }
                sb.append(member.getPersonalTitle().getValue()).append(" ")
                    .append(member.getAffiliationStatement().getDisplayPersonName())
                    .append(", ").append(member.getEmploymentTitle().getValue());

                member.getInstitutions().stream().findFirst()
                    .ifPresent(
                        inst -> sb.append(" ").append(getTransliteratedContent(inst.getName())));
            });
        dto.setCommission(sb.toString().trim());
    }

    private void populateInstitutionInformation(Thesis phdThesis,
                                                PhdThesisPrePopulatedDataDTO dto) {
        if (Objects.nonNull(phdThesis.getOrganisationUnit())) {
            dto.setInstitutionName(
                getTransliteratedContent(phdThesis.getOrganisationUnit().getName()));
            dto.setPlace(SerbianTransliteration.toCyrillic(
                phdThesis.getOrganisationUnit().getLocation().getAddress()));
        } else {
            dto.setInstitutionName(
                getTransliteratedContent(phdThesis.getExternalOrganisationUnitName()));
        }
    }

    private String getTransliteratedContent(Set<MultiLingualContent> multilingualContent) {
        MultiLingualContent fallback = null;
        for (MultiLingualContent content : multilingualContent) {
            if ("SR".equalsIgnoreCase(content.getLanguage().getLanguageTag())) {
                return SerbianTransliteration.toCyrillic(content.getContent());
            }
            fallback = content;
        }
        return fallback != null ? fallback.getContent() : "";
    }
}
