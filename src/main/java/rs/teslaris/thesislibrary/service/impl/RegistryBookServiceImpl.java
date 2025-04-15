package rs.teslaris.thesislibrary.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.person.PersonNameConverter;
import rs.teslaris.core.converter.person.PostalAddressConverter;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.exceptionhandling.exception.PromotionException;
import rs.teslaris.core.util.exceptionhandling.exception.RegistryBookException;
import rs.teslaris.core.util.exceptionhandling.exception.ThesisException;
import rs.teslaris.core.util.language.SerbianTransliteration;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;
import rs.teslaris.thesislibrary.converter.RegistryBookEntryConverter;
import rs.teslaris.thesislibrary.dto.DissertationInformationDTO;
import rs.teslaris.thesislibrary.dto.InstitutionCountsReportDTO;
import rs.teslaris.thesislibrary.dto.PhdThesisPrePopulatedDataDTO;
import rs.teslaris.thesislibrary.dto.PreviousTitleInformationDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookContactInformationDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookEntryDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookPersonalInformationDTO;
import rs.teslaris.thesislibrary.model.DissertationInformation;
import rs.teslaris.thesislibrary.model.PreviousTitleInformation;
import rs.teslaris.thesislibrary.model.Promotion;
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

    private final OrganisationUnitService organisationUnitService;

    private final CountryService countryService;

    private final PromotionService promotionService;

    private final ThesisService thesisService;

    private final EmailUtil emailUtil;

    private final UserRepository userRepository;

    private final NotificationService notificationService;

    private final MessageSource messageSource;

    private final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.GERMANY);

    @Value("${frontend.application.address}")
    private String clientAppAddress;


    @Override
    protected JpaRepository<RegistryBookEntry, Integer> getEntityRepository() {
        return registryBookEntryRepository;
    }

    @Override
    public RegistryBookEntryDTO readRegistryBookEntry(Integer registryBookEntryId) {
        return RegistryBookEntryConverter.toDTO(findOne(registryBookEntryId));
    }

    @Override
    public Page<RegistryBookEntryDTO> getNonPromotedRegistryBookEntries(Integer userId,
                                                                        Pageable pageable) {
        var userEmploymentInstitutionId = userRepository.findOrganisationUnitIdForUser(userId);
        if (Objects.nonNull(userEmploymentInstitutionId) && userEmploymentInstitutionId > 0) {
            registryBookEntryRepository.getNonPromotedBookEntries(
                organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
                    userEmploymentInstitutionId), pageable).map(RegistryBookEntryConverter::toDTO);
        }

        return registryBookEntryRepository.getNonPromotedBookEntries(pageable)
            .map(RegistryBookEntryConverter::toDTO);
    }

    @Override
    public Page<RegistryBookEntryDTO> getRegistryBookEntriesForPromotion(Integer promotionId,
                                                                         Pageable pageable) {
        return registryBookEntryRepository.getBookEntriesForPromotion(promotionId, pageable)
            .map(RegistryBookEntryConverter::toDTO);
    }

    @Override
    public RegistryBookEntry createRegistryBookEntry(RegistryBookEntryDTO dto, Integer thesisId) {
        var newEntry = new RegistryBookEntry();
        var existingEntryId = registryBookEntryRepository.hasThesisRegistryBookEntry(thesisId);
        if (Objects.nonNull(existingEntryId) && existingEntryId > 0) {
            throw new RegistryBookException("Entry with this thesis is already created.");
        }

        var thesis = thesisService.getThesisById(thesisId);

        newEntry.setThesis(thesisService.getThesisById(thesisId));
        setCommonFields(newEntry, dto);

        if (Objects.nonNull(dto.getDissertationInformation().getOrganisationUnitId())) {
            newEntry.getDissertationInformation().setOrganisationUnit(thesis.getOrganisationUnit());
        }

        return save(newEntry);
    }

    @Override
    public void updateRegistryBookEntry(Integer registryBookEntryId,
                                        RegistryBookEntryDTO dto) {
        var entry = findOne(registryBookEntryId);

        if (Objects.nonNull(entry.getPromotion()) && entry.getPromotion().getFinished() &&
            !entry.getAllowSingleEdit()) {
            throw new RegistryBookException("Can't update a promoted entry.");
        }

        var presetOrgUnit = entry.getDissertationInformation().getOrganisationUnit();
        setCommonFields(entry, dto);
        entry.getDissertationInformation().setOrganisationUnit(presetOrgUnit);
        entry.setAllowSingleEdit(false);
        save(entry);
    }

    @Override
    public void deleteRegistryBookEntry(Integer registryBookEntryId) {
        var entry = findOne(registryBookEntryId);

        if (Objects.nonNull(entry.getPromotion())) {
            throw new RegistryBookException("Can't delete entry that has a promotion.");
        }

        registryBookEntryRepository.delete(entry);
    }

    private void setCommonFields(RegistryBookEntry entry, RegistryBookEntryDTO dto) {
        entry.setDissertationInformation(
            toDissertationInformation(dto.getDissertationInformation()));
        entry.setPersonalInformation(toPersonalInformation(dto.getPersonalInformation()));
        entry.setContactInformation(toContactInformation(dto.getContactInformation()));
        entry.setPreviousTitleInformation(
            toPreviousTitleInformation(dto.getPreviousTitleInformation()));
    }

    private DissertationInformation toDissertationInformation(DissertationInformationDTO dto) {
        var di = new DissertationInformation();
        di.setDissertationTitle(dto.getDissertationTitle());

        di.setMentor(dto.getMentor());
        di.setCommission(dto.getCommission());
        di.setGrade(dto.getGrade());
        di.setAcquiredTitle(dto.getAcquiredTitle());
        di.setDefenceDate(dto.getDefenceDate());
        di.setDiplomaNumber(dto.getDiplomaNumber());
        di.setDiplomaIssueDate(dto.getDiplomaIssueDate());
        di.setDiplomaSupplementsNumber(dto.getDiplomaSupplementsNumber());
        di.setDiplomaSupplementsIssueDate(dto.getDiplomaSupplementsIssueDate());
        di.setInstitutionPlace(dto.getInstitutionPlace());
        return di;
    }

    private RegistryBookPersonalInformation toPersonalInformation(
        RegistryBookPersonalInformationDTO dto) {
        if (dto.getMotherName().isBlank() && dto.getFatherName().isBlank() &&
            dto.getGuardianNameAndSurname().isBlank()) {
            throw new RegistryBookException(
                "You have to provide at least one parent's or guardian's name.");
        }

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

    @Override
    public void addToPromotion(Integer registryBookEntryId, Integer promotionId) {
        var entry = findOne(registryBookEntryId);

        if (Objects.nonNull(entry.getPromotion())) {
            throw new PromotionException("Already in promotion.");
        }

        entry.setPromotion(promotionService.findOne(promotionId));
        entry.setAttendanceIdentifier(UUID.randomUUID().toString());
        save(entry);

        notifyCandidate(entry.getPromotion(), entry, true, entry.getAttendanceIdentifier(), "sr");
    }

    @Override
    public void removeFromPromotion(Integer registryBookEntryId) {
        var entry = findOne(registryBookEntryId);

        if (Objects.isNull(entry.getPromotion()) || entry.getPromotion().getFinished()) {
            throw new PromotionException("Already not in ongoing promotion.");
        }

        performRemoval(entry, entry.getPromotion());
    }

    @Override
    public void removeFromPromotion(String attendanceIdentifier) {
        var entry = registryBookEntryRepository.findByAttendanceIdentifier(attendanceIdentifier);

        if (entry.isEmpty() || Objects.isNull(entry.get().getPromotion())) {
            throw new PromotionException("Already not in promotion.");
        }

        var promotionDate = entry.get().getPromotion().getPromotionDate();
        performRemoval(entry.get(), entry.get().getPromotion());

        var adminUsersToNotify = userRepository.findAllRegistryAdmins();
        adminUsersToNotify.addAll(
            userRepository.findAllSystemAdmins()); // TODO: Should we notify system admin(s) as well?
        adminUsersToNotify.forEach(userToNotify -> {
            if (Objects.nonNull(userToNotify.getOrganisationUnit()) &&
                !organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
                        userToNotify.getOrganisationUnit().getId())
                    .contains(
                        entry.get().getDissertationInformation().getOrganisationUnit().getId())) {
                return;
            }
            notificationService.createNotification(
                NotificationFactory.contructCandidatePulledFromPromotionNotification(
                    Map.of("candidateName",
                        entry.get().getPersonalInformation().getAuthorName().toString(),
                        "promotionDate", promotionDate.format(DATE_FORMATTER)),
                    userToNotify)
            );
        });
    }

    @Override
    public boolean isAttendanceNotCancelled(String attendanceIdentifier) {
        var entry = registryBookEntryRepository.findByAttendanceIdentifier(attendanceIdentifier);
        return entry.isPresent();
    }

    private void performRemoval(RegistryBookEntry entry, Promotion promotion) {
        entry.setPromotion(null);
        entry.setAttendanceIdentifier(null);
        save(entry);

        // TODO: Language is hardcoded for now, should we make it parametrized somewhere?
        notifyCandidate(promotion, entry, false, entry.getAttendanceIdentifier(), "sr");
    }

    private void notifyCandidate(Promotion promotion, RegistryBookEntry entry, boolean added,
                                 String attendanceToken, String lang) {
        String emailSubject, emailBody;

        if (Objects.isNull(entry.getContactInformation().getContact()) ||
            entry.getContactInformation().getContact().getContactEmail().isEmpty()) {
            return;
        }

        if (added) {
            var cancellationLink =
                clientAppAddress + (clientAppAddress.endsWith("/") ? "sr" : "/" + "sr") +
                    "/cancel-attendance/" + attendanceToken;
            emailSubject = messageSource.getMessage(
                "promotion.inviteEmailSubject",
                new Object[] {entry.getPersonalInformation().getAuthorName().toString()},
                Locale.forLanguageTag(lang)
            );
            emailBody = messageSource.getMessage(
                "promotion.inviteEmailBody",
                new Object[] {entry.getPersonalInformation().getAuthorName().toString(),
                    promotion.getPlaceOrVenue(),
                    promotion.getPromotionDate().format(DATE_FORMATTER),
                    promotion.getPromotionTime(), cancellationLink},
                Locale.forLanguageTag(lang));
        } else {
            emailSubject = messageSource.getMessage(
                "promotion.cancelConfirmationSubject",
                new Object[] {},
                Locale.forLanguageTag(lang)
            );
            emailBody = messageSource.getMessage(
                "promotion.cancelConfirmationBody",
                new Object[] {entry.getPersonalInformation().getAuthorName().toString(),
                    promotion.getPlaceOrVenue(),
                    promotion.getPromotionDate().format(DATE_FORMATTER),
                    promotion.getPromotionTime()},
                Locale.forLanguageTag(lang));
        }

        emailUtil.sendSimpleEmail(entry.getContactInformation().getContact().getContactEmail(),
            emailSubject, emailBody);
    }

    @Override
    public void promoteAll(Integer promotionId) {
        var promotion = promotionService.findOne(promotionId);
        var finalPromotionSchoolYear = getPromotionSchoolYear(promotion);
        AtomicInteger registryBookNumber = new AtomicInteger(
            Objects.requireNonNullElse(registryBookEntryRepository.getLastRegistryBookNumber(
                promotion.getInstitution().getId()), 0) + 1);

        var entriesToPromote =
            registryBookEntryRepository.getBookEntriesForPromotion(promotionId, Pageable.unpaged());
        if (entriesToPromote.isEmpty()) {
            throw new PromotionException("Can't promote empty promotion.");
        }

        entriesToPromote
            .forEach(registryBookEntry -> {
                if (Objects.requireNonNullElse(
                        registryBookEntry.getDissertationInformation().getDiplomaNumber(), "")
                    .isBlank() || Objects.isNull(
                    registryBookEntry.getDissertationInformation().getDiplomaIssueDate())) {
                    throw new RegistryBookException("missingDiplomaMetadataMessage");
                }

                registryBookEntry.setPromotionSchoolYear(finalPromotionSchoolYear);
                registryBookEntry.setRegistryBookNumber(registryBookNumber.getAndIncrement());
                registryBookEntry.setAttendanceIdentifier(null);
                registryBookEntry.setRegistryBookInstitution(promotion.getInstitution());

                registryBookEntryRepository.save(registryBookEntry);
            });

        promotion.setFinished(true);
        promotionService.save(promotion);
    }

    @Override
    public Integer hasThesisRegistryBookEntry(Integer thesisId) {
        return registryBookEntryRepository.hasThesisRegistryBookEntry(thesisId);
    }

    @Override
    public List<String> getPromoteesList(Integer promotionId) {
        var promotees = new ArrayList<String>();
        registryBookEntryRepository.getBookEntriesForPromotion(promotionId, Pageable.unpaged())
            .forEach(entry -> {
                String sb = entry.getPersonalInformation().getAuthorName().toString() + ", " +
                    entry.getDissertationInformation().getAcquiredTitle() +
                    " (email: " +
                    entry.getContactInformation().getContact().getContactEmail() + ")\n";
                promotees.add(sb);
            });

        return promotees;
    }

    @Override
    public List<String> getAddressesList(Integer promotionId) {
        var addresses = new ArrayList<String>();
        registryBookEntryRepository.getBookEntriesForPromotion(promotionId, Pageable.unpaged())
            .forEach(entry -> {
                var contactInfo = entry.getContactInformation();
                String sb = entry.getPersonalInformation().getAuthorName().toString() + "\n" +
                    contactInfo.getStreetAndNumber() + "\n" +
                    contactInfo.getPlace() + "\n" +
                    contactInfo.getPostalCode() + "\n" +
                    getTransliteratedContent(contactInfo.getResidenceCountry().getName()) +
                    "\n" +
                    contactInfo.getContact().getContactEmail() + "\n" +
                    "tel: " + contactInfo.getContact().getPhoneNumber() + "\n\n";
                addresses.add(sb);
            });

        return addresses;
    }

    @Override
    public Page<RegistryBookEntryDTO> getRegistryBookForInstitutionAndPeriod(Integer userId,
                                                                             Integer institutionId,
                                                                             LocalDate from,
                                                                             LocalDate to,
                                                                             Pageable pageable) {
        var userEmploymentInstitutionId = userRepository.findOrganisationUnitIdForUser(userId);
        if (Objects.nonNull(userEmploymentInstitutionId) && userEmploymentInstitutionId > 0 &&
            !organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
                userEmploymentInstitutionId).contains(institutionId)) {
            throw new CantEditException(
                "You don't have rights to view this institution's registry book.");
        }

        return registryBookEntryRepository.getRegistryBookEntriesForInstitutionAndPeriod(
            institutionId, from, to, pageable).map(RegistryBookEntryConverter::toDTO);
    }

    public List<InstitutionCountsReportDTO> institutionCountsReport(
        Integer userId,
        LocalDate from,
        LocalDate to) {
        List<Integer> institutionIds = getInstitutionIdsForUser(userId);
        Map<Integer, Pair<Integer, Integer>> countTable =
            getCountsForInstitutions(institutionIds, from, to);

        return countTable.entrySet().stream()
            .map(entry -> new InstitutionCountsReportDTO(
                MultilingualContentConverter.getMultilingualContentDTO(
                    organisationUnitService.findOne(entry.getKey()).getName()),
                entry.getValue()))
            .collect(Collectors.toList());
    }

    @Override
    public void allowSingleUpdate(Integer registryBookEntryId) {
        var entry = findOne(registryBookEntryId);

        if (!entry.getPromotion().getFinished()) {
            throw new RegistryBookException("Entry is not promoted.");
        }

        entry.setAllowSingleEdit(true);
        save(entry);
    }

    @Override
    public boolean canEdit(Integer registryBookEntryId) {
        var entry = findOne(registryBookEntryId);
        return Objects.isNull(entry.getPromotion()) || !entry.getPromotion().getFinished() ||
            entry.getAllowSingleEdit();
    }

    private List<Integer> getInstitutionIdsForUser(Integer userId) {
        Integer topLevelInstitutionId = userRepository.findOrganisationUnitIdForUser(userId);

        if (Objects.isNull(topLevelInstitutionId) || topLevelInstitutionId < 1) {
            return userRepository.findAllRegistryAdmins().stream()
                .map(admin -> userRepository.findOrganisationUnitIdForUser(admin.getId()))
                .collect(Collectors.toList());
        } else {
            return organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
                topLevelInstitutionId);
        }
    }

    private Map<Integer, Pair<Integer, Integer>> getCountsForInstitutions(
        List<Integer> institutionIds,
        LocalDate from,
        LocalDate to) {

        Map<Integer, Pair<Integer, Integer>> countTable = new HashMap<>();

        for (Integer institutionId : institutionIds) {
            int newPromotionCount = registryBookEntryRepository
                .getRegistryBookCountForInstitutionAndPeriodNewPromotion(institutionId, from, to);

            int oldPromotionCount = registryBookEntryRepository
                .getRegistryBookCountForInstitutionAndPeriodOldPromotion(institutionId, from, to);

            countTable.put(institutionId, new Pair<>(newPromotionCount, oldPromotionCount));
        }

        return countTable;
    }

    @NotNull
    private String getPromotionSchoolYear(Promotion promotion) {
        if (promotion.getFinished()) {
            throw new PromotionException("Promotion is already finished.");
        }

        var promotionSchoolYear = "";
        var promotionYear = promotion.getPromotionDate().getYear();
        if (promotion.getPromotionDate().isBefore(LocalDate.of(promotionYear, 10, 1))) {
            promotionSchoolYear = (promotionYear - 1) + "/" + promotionYear;
        } else {
            promotionSchoolYear = promotionYear + "/" + (promotionYear + 1);
        }

        return promotionSchoolYear;
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
                    .ifPresentOrElse(
                        inst -> sb.append(" ").append(getTransliteratedContent(inst.getName())),
                        () -> sb.append(" ").append(getTransliteratedContent(
                            mentor.getAffiliationStatement().getDisplayAffiliationStatement())));

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
                    .ifPresentOrElse(
                        inst -> sb.append(" ").append(getTransliteratedContent(inst.getName())),
                        () -> sb.append(" ").append(getTransliteratedContent(
                            member.getAffiliationStatement().getDisplayAffiliationStatement())));
            });
        dto.setCommission(sb.toString().trim());
    }

    private void populateInstitutionInformation(Thesis phdThesis,
                                                PhdThesisPrePopulatedDataDTO dto) {
        if (Objects.nonNull(phdThesis.getOrganisationUnit())) {
            dto.setInstitutionName(
                getTransliteratedContent(phdThesis.getOrganisationUnit().getName()));
            dto.setInstitutionId(phdThesis.getOrganisationUnit().getId());
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
