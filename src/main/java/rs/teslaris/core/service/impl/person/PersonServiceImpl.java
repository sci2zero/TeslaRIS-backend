package rs.teslaris.core.service.impl.person;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.converter.person.InvolvementConverter;
import rs.teslaris.core.converter.person.PersonConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.PersonIdentifierable;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.dto.person.PersonUserResponseDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.dto.person.involvement.InvolvementDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.document.PersonContributionRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonNameService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.IdentifierUtil;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.PersonReferenceConstraintViolationException;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.StringUtil;

@Service
@RequiredArgsConstructor
public class PersonServiceImpl extends JPAServiceImpl<Person> implements PersonService {

    private final PersonRepository personRepository;

    private final SearchService<PersonIndex> searchService;

    private final ExpressionTransformer expressionTransformer;

    private final PersonIndexRepository personIndexRepository;

    private final OrganisationUnitService organisationUnitService;

    private final CountryService countryService;

    private final LanguageTagService languageTagService;

    private final PersonNameService personNameService;

    private final PersonContributionRepository personContributionRepository;

    private final IndexBulkUpdateService indexBulkUpdateService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final MultilingualContentService multilingualContentService;

    private final FileService fileService;

    @Value("${person.approved_by_default}")
    private Boolean approvedByDefault;


    private static boolean validateImageMIMEType(MultipartFile multipartFile) throws IOException {
        var validMimeTypes = List.of("image/jpeg", "image/png");

        String contentType = multipartFile.getContentType();
        if (!validMimeTypes.contains(contentType)) {
            return false;
        }

        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null ||
            !(originalFilename.endsWith(".jpg") || originalFilename.endsWith(".jpeg") ||
                originalFilename.endsWith(".png"))) {
            return false;
        }

        var tika = new Tika();
        String detectedType = tika.detect(multipartFile.getInputStream());
        return List.of("image/jpeg", "image/png").contains(detectedType);
    }

    @Override
    protected JpaRepository<Person, Integer> getEntityRepository() {
        return personRepository;
    }

    @Override
    @Deprecated(forRemoval = true)
    public Person findPersonById(Integer id) {
        return personRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Person with given ID does not exist."));
    }

    @Override
    @Nullable
    public PersonResponseDTO readPersonByScopusId(String scopusAuthorId) {
        var personOptional = personRepository.findPersonByScopusAuthorId(scopusAuthorId);

        return personOptional.map(PersonConverter::toDTO).orElse(null);
    }

    @Override
    @Nullable
    public Optional<User> findUserByScopusAuthorId(String scopusAuthorId) {
        return personRepository.findUserForPersonScopusId(scopusAuthorId);
    }

    @Override
    @Nullable
    public Person findPersonByOldId(Integer oldId) {
        return personRepository.findPersonByOldId(oldId).orElse(null);
    }

    @Override
    @Transactional
    public PersonResponseDTO readPersonWithBasicInfo(Integer id) {
        var person = personRepository.findApprovedPersonById(id)
            .orElseThrow(() -> new NotFoundException("Person with given ID does not exist."));
        return PersonConverter.toDTO(person);
    }

    @Override
    @Transactional
    public PersonResponseDTO readPersonWithBasicInfoForOldId(Integer oldId) {
        var personToReturn = findPersonByOldId(oldId);

        if (Objects.isNull(personToReturn)) {
            throw new NotFoundException("Person with given 'OLD ID' does not exist.");
        }

        return PersonConverter.toDTO(personToReturn);
    }

    @Override
    @Transactional
    public PersonUserResponseDTO readPersonWithUser(Integer id) {
        var person = personRepository.findApprovedPersonByIdWithUser(id)
            .orElseThrow(() -> new NotFoundException("Person with given ID does not exist."));
        return PersonConverter.toDTOWithUser(person);
    }

    @Override
    @Transactional
    public boolean isPersonEmployedInOrganisationUnit(Integer personId,
                                                      Integer organisationUnitId) {
        var person = findOne(personId);

        for (var personInvolvement : person.getInvolvements()) {
            Integer personOrganisationUnitId = personInvolvement.getOrganisationUnit().getId();
            if (personInvolvement.getInvolvementType() == InvolvementType.EMPLOYED_AT &&
                Objects.equals(personOrganisationUnitId, organisationUnitId)) {
                return true;
            }

            if (organisationUnitService.recursiveCheckIfOrganisationUnitBelongsTo(
                organisationUnitId, personOrganisationUnitId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    @Transactional
    public Person createPersonWithBasicInfo(BasicPersonDTO personDTO, Boolean index) {
        var defaultApproveStatus =
            approvedByDefault ? ApproveStatus.APPROVED : ApproveStatus.REQUESTED;

        var personNameDTO = personDTO.getPersonName();
        var personName = new PersonName(personNameDTO.getFirstname(), personNameDTO.getOtherName(),
            personNameDTO.getLastname(), personDTO.getLocalBirthDate(), null);

        var personalContact = new Contact(personDTO.getContactEmail(), personDTO.getPhoneNumber());
        var personalInfo = new PersonalInfo(personDTO.getLocalBirthDate(), null, personDTO.getSex(),
            new PostalAddress(null, new HashSet<>(), new HashSet<>()), personalContact,
            new HashSet<>());

        var newPerson = new Person();
        newPerson.setName(personName);
        newPerson.setPersonalInfo(personalInfo);

        setAllPersonIdentifiers(newPerson, personDTO);
        newPerson.setOldId(personDTO.getOldId());

        if (Objects.nonNull(personDTO.getOrganisationUnitId())) {
            var employmentInstitution =
                organisationUnitService.findOrganisationUnitById(personDTO.getOrganisationUnitId());
            var currentEmployment =
                new Employment(null, null, defaultApproveStatus, new HashSet<>(),
                    InvolvementType.EMPLOYED_AT, new HashSet<>(), null, employmentInstitution,
                    personDTO.getEmploymentPosition(), new HashSet<>());
            newPerson.addInvolvement(currentEmployment);
        }

        newPerson.setApproveStatus(defaultApproveStatus);

        var savedPerson = this.save(newPerson);
        newPerson.setId(savedPerson.getId());

        if (savedPerson.getApproveStatus().equals(ApproveStatus.APPROVED) && index) {
            indexPerson(savedPerson, savedPerson.getId());
        }

        return newPerson;
    }

    @Override
    @Transactional
    public void setPersonBiography(List<MultilingualContentDTO> biographyDTO, Integer personId) {
        var personToUpdate = findOne(personId);
        personToUpdate.getBiography().clear();
        biographyDTO.stream().map(biography -> {
            var languageTag = languageTagService.findOne(biography.getLanguageTagId());
            return new MultiLingualContent(languageTag, biography.getContent(),
                biography.getPriority());
        }).forEach(biography -> {
            personToUpdate.getBiography().add(biography);
            this.save(personToUpdate);
        });
    }

    @Override
    @Transactional
    public void setPersonKeyword(List<MultilingualContentDTO> keywordDTO, Integer personId) {
        var personToUpdate = findOne(personId);
        personToUpdate.getKeyword().clear();
        keywordDTO.stream().map(keyword -> {
            var languageTag = languageTagService.findOne(keyword.getLanguageTagId());
            return new MultiLingualContent(languageTag, keyword.getContent(),
                keyword.getPriority());
        }).forEach(keyword -> {
            personToUpdate.getKeyword().add(keyword);
            this.save(personToUpdate);
        });
    }

    @Override
    @Transactional
    public void updatePersonMainName(Integer personId, PersonNameDTO personNameDTO) {
        var personToUpdate = findOne(personId);

        personToUpdate.getName().setFirstname(personNameDTO.getFirstname());
        personToUpdate.getName().setOtherName(personNameDTO.getOtherName());
        personToUpdate.getName().setLastname(personNameDTO.getLastname());
        personToUpdate.getName().setDateFrom(personNameDTO.getDateFrom());
        personToUpdate.getName().setDateTo(personNameDTO.getDateTo());

        save(personToUpdate);

        if (personToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            indexPerson(personToUpdate, personToUpdate.getId());
        }
    }

    @Override
    @Transactional
    public void setPersonMainName(Integer personNameId, Integer personId) {
        var personToUpdate = findOne(personId);
        var chosenName = personNameService.findOne(personNameId);

        personToUpdate.getOtherNames().add(personToUpdate.getName());
        personToUpdate.setName(chosenName);
        personToUpdate.getOtherNames().remove(chosenName);

        this.save(personToUpdate);

        if (personToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            indexPerson(personToUpdate, personToUpdate.getId());
        }
    }

    @Override
    @Transactional
    public void setPersonOtherNames(List<PersonNameDTO> personNameDTO, Integer personId) {
        var personToUpdate = findOne(personId);

        var personNameIds = personToUpdate.getOtherNames().stream().map(PersonName::getId)
            .collect(Collectors.toList());

        personToUpdate.getOtherNames().clear();
        personNameService.deletePersonNamesWithIds(personNameIds);

        personNameDTO.stream().map(
                personName -> new PersonName(personName.getFirstname(), personName.getOtherName(),
                    personName.getLastname(), personName.getDateFrom(), personName.getDateTo()))
            .forEach(personName -> {
                personToUpdate.getOtherNames().add(personName);
                personRepository.save(personToUpdate);
            });

        save(personToUpdate);
        if (personToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            indexPerson(personToUpdate, personToUpdate.getId());
        }
    }

    @Override
    @Transactional
    public void updatePersonalInfo(Integer personId, PersonalInfoDTO personalInfo) {
        var personToUpdate = findOne(personId);
        setAllPersonIdentifiers(personToUpdate, personalInfo);

        var personalInfoToUpdate = personToUpdate.getPersonalInfo();
        personalInfoToUpdate.setPlaceOfBrith(personalInfo.getPlaceOfBirth());
        personalInfoToUpdate.setLocalBirthDate(personalInfo.getLocalBirthDate());
        personalInfoToUpdate.setSex(personalInfo.getSex());
        IdentifierUtil.setUris(personalInfoToUpdate.getUris(), personalInfo.getUris());

        var countryId = personalInfo.getPostalAddress().getCountryId();

        personalInfoToUpdate.getPostalAddress()
            .setCountry(countryId != null ? countryService.findOne(countryId) : null);

        personToUpdate.getPersonalInfo().getPostalAddress().getStreetAndNumber().clear();
        setPersonStreetAndNumberInfo(personToUpdate, personalInfoToUpdate, personalInfo);

        personToUpdate.getPersonalInfo().getPostalAddress().getCity().clear();
        setPersonCityInfo(personToUpdate, personalInfoToUpdate, personalInfo);

        if (Objects.nonNull(personalInfo.getContact())) {
            if (Objects.isNull(personalInfoToUpdate.getContact())) {
                personalInfoToUpdate.setContact(new Contact());
            }

            personalInfoToUpdate.getContact()
                .setContactEmail(personalInfo.getContact().getContactEmail());
            personalInfoToUpdate.getContact()
                .setPhoneNumber(personalInfo.getContact().getPhoneNumber());
        }

        save(personToUpdate);

        if (personToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            indexPerson(personToUpdate, personToUpdate.getId());
        }
    }

    @Override
    public void approvePerson(Integer personId, Boolean approve) {
        var personToBeApproved = findOne(personId);

        var approveStatus = approve ? ApproveStatus.APPROVED : ApproveStatus.DECLINED;
        if (personToBeApproved.getApproveStatus().equals(ApproveStatus.REQUESTED)) {
            personToBeApproved.setApproveStatus(approveStatus);
        }

        var approvedPerson = this.save(personToBeApproved);

        if (approve) {
            indexPerson(approvedPerson, approvedPerson.getId());
        }
    }

    @Override
    public void deletePerson(Integer personId) {
        if (personRepository.hasContribution(personId) ||
            personRepository.isBoundToUser(personId)) {
            throw new PersonReferenceConstraintViolationException(
                "This person is already in use.");
        }

        delete(personId);
        var index = personIndexRepository.findByDatabaseId(personId);
        index.ifPresent(personIndexRepository::delete);
    }

    @Override
    @Transactional
    public void forceDeletePerson(Integer personId) {
        if (personRepository.isBoundToUser(personId)) {
            throw new PersonReferenceConstraintViolationException(
                "This person is already in use.");
        }

        personContributionRepository.deletePersonEventContributions(personId);
        personContributionRepository.deletePersonPublicationsSeriesContributions(personId);

        deletePersonPublications(personId, false);

        delete(personId);

        var index = personIndexRepository.findByDatabaseId(personId);
        index.ifPresent(personIndexRepository::delete);

        deleteOrUnbindPersonRelatedIndexes(personId);
    }

    @Override
    @Transactional
    public void switchToUnmanagedEntity(Integer personId) {
        if (personRepository.isBoundToUser(personId)) {
            throw new PersonReferenceConstraintViolationException(
                "This person is already in use.");
        }

        personContributionRepository.deleteInstitutionsForForPersonContributions(personId);
        personContributionRepository
            .makePersonEventContributionsPointToExternalContributor(personId);
        personContributionRepository
            .makePersonPublicationsSeriesContributionsPointToExternalContributor(personId);

        deletePersonPublications(personId, true);

        delete(personId);

        var index = personIndexRepository.findByDatabaseId(personId);
        index.ifPresent(personIndexRepository::delete);
    }

    @Override
    public String setPersonProfileImage(Integer personId, MultipartFile multipartFile)
        throws IOException {
        if (!validateImageMIMEType(multipartFile)) {
            throw new IllegalArgumentException("mimeTypeValidationFailed");
        }

        var person = findOne(personId);

        if (Objects.nonNull(person.getProfileImageServerName())) {
            fileService.delete(person.getProfileImageServerName());
        }

        var serverFilename = fileService.store(multipartFile, UUID.randomUUID().toString());

        person.setProfileImageServerName(serverFilename);

        save(person);

        return serverFilename;
    }

    public void deletePersonPublications(Integer personId, boolean switchToUnmanaged) {
        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<PersonDocumentContribution> chunk =
                personContributionRepository.fetchAllPersonDocumentContributions(personId,
                    PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((contribution) -> {
                var index =
                    documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                        contribution.getDocument().getId());
                if (index.isEmpty()) {
                    contribution.getDocument()
                        .setDeleted(true); // is this sound, this should never happen?
                    return;
                }

                if (switchToUnmanaged) {
                    contribution.setPerson(null);

                    BiConsumer<List<Integer>, Integer> replaceIdWithUnmanaged =
                        (list, idToReplace) ->
                            list.replaceAll(id -> id.equals(idToReplace) ? -1 : id);

                    List<Supplier<List<Integer>>> idLists = List.of(
                        index.get()::getAuthorIds,
                        index.get()::getEditorIds,
                        index.get()::getReviewerIds,
                        index.get()::getBoardMemberIds,
                        index.get()::getAdvisorIds
                    );

                    idLists.forEach(
                        idList -> replaceIdWithUnmanaged.accept(idList.get(), personId));

                    contribution.getInstitutions().clear();
                    documentPublicationIndexRepository.save(index.get());
                    return;
                }

                if (index.get().getType().equals(DocumentPublicationType.MONOGRAPH.name()) ||
                    index.get().getType().equals(DocumentPublicationType.PROCEEDINGS.name())) {
                    contribution.setDeleted(true);
                    contribution.getInstitutions().forEach(institution -> {
                        index.get().getOrganisationUnitIds().remove(institution.getId());
                    });
                    contribution.getInstitutions().clear();

                    documentPublicationIndexRepository.save(index.get());
                } else {
                    contribution.getDocument().setDeleted(true);
                }
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    public void deleteOrUnbindPersonRelatedIndexes(Integer personId) {
        documentPublicationIndexRepository.deleteByAuthorIdsAndType(personId,
            DocumentPublicationType.JOURNAL_PUBLICATION.name());
        documentPublicationIndexRepository.deleteByAuthorIdsAndType(personId,
            DocumentPublicationType.PROCEEDINGS_PUBLICATION.name());
        documentPublicationIndexRepository.deleteByAuthorIdsAndType(personId,
            DocumentPublicationType.MONOGRAPH_PUBLICATION.name());
        documentPublicationIndexRepository.deleteByAuthorIdsAndType(personId,
            DocumentPublicationType.SOFTWARE.name());
        documentPublicationIndexRepository.deleteByAuthorIdsAndType(personId,
            DocumentPublicationType.DATASET.name());
        documentPublicationIndexRepository.deleteByAuthorIdsAndType(personId,
            DocumentPublicationType.PATENT.name());
        documentPublicationIndexRepository.deleteByAuthorIdsAndType(personId,
            DocumentPublicationType.THESIS.name());

        indexBulkUpdateService.removeIdFromListAndRelatedArrayField("document_publication",
            "author_ids", "author_names", "author_names_sortable", personId);
    }

    @Nullable
    public Involvement getLatestResearcherInvolvement(Person person) {
        if (Objects.nonNull(person.getInvolvements())) {
            Optional<Involvement> latestInvolvement = person.getInvolvements().stream()
                .filter(involvement -> Objects.nonNull(involvement.getOrganisationUnit()))
                .filter(involvement ->
                    involvement.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                        involvement.getInvolvementType().equals(InvolvementType.HIRED_BY) ||
                        involvement.getInvolvementType().equals(InvolvementType.MEMBER_OF))
                .max(Comparator.comparing((involvement) -> {
                    if (Objects.nonNull(involvement.getDateFrom())) {
                        return involvement.getDateFrom();
                    }

                    return LocalDate.now(); // Look at it as most recent involvement
                }));

            if (latestInvolvement.isPresent()) {
                return latestInvolvement.get();
            }
        }
        return null;
    }

    @Override
    @Transactional
    public InvolvementDTO getLatestResearcherInvolvement(Integer personId) {
        var person = findOne(personId);
        var latestInvolvement = getLatestResearcherInvolvement(person);
        return Objects.nonNull(latestInvolvement) ? InvolvementConverter.toDTO(latestInvolvement) :
            null;
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexPersons() {
        personIndexRepository.deleteAll();
        int pageNumber = 0;
        int chunkSize = 50;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<Person> chunk = findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((person) -> indexPerson(person, person.getId()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    @Transactional
    private void setPersonStreetAndNumberInfo(Person personToUpdate,
                                              PersonalInfo personalInfoToUpdate,
                                              PersonalInfoDTO personalInfo) {
        personalInfo.getPostalAddress().getStreetAndNumber().stream().map(streetAndNumber -> {
            var languageTag =
                languageTagService.findOne(streetAndNumber.getLanguageTagId());
            return new MultiLingualContent(languageTag, streetAndNumber.getContent(),
                streetAndNumber.getPriority());
        }).forEach(streetAndNumberContent -> {
            personalInfoToUpdate.getPostalAddress().getStreetAndNumber()
                .add(streetAndNumberContent);
            this.save(personToUpdate);
        });
    }

    @Transactional
    private void setPersonCityInfo(Person personToUpdate, PersonalInfo personalInfoToUpdate,
                                   PersonalInfoDTO personalInfo) {
        personalInfo.getPostalAddress().getCity().stream().map(city -> {
            var languageTag = languageTagService.findOne(city.getLanguageTagId());
            return new MultiLingualContent(languageTag, city.getContent(), city.getPriority());
        }).forEach(city -> {
            personalInfoToUpdate.getPostalAddress().getCity().add(city);
            this.save(personToUpdate);
        });
    }

    @Override
    public void indexPerson(Person savedPerson, Integer personDatabaseId) {
        var personIndex = getPersonIndexForId(personDatabaseId);

        setPersonIndexProperties(personIndex, savedPerson);

        setPersonIndexEmploymentDetails(personIndex, savedPerson);

        personIndexRepository.save(personIndex);
    }

    @Override
    public Integer getPersonIdForUserId(Integer userId) {
        return personRepository.findPersonIdForUserId(userId).orElse(null);
    }

    @Override
    public List<Integer> findInstitutionIdsForPerson(Integer personId) {
        return personRepository.findInstitutionIdsForPerson(personId);
    }

    @Override
    public boolean isPersonBoundToAUser(Integer personId) {
        return personRepository.isBoundToUser(personId);
    }

    @Override
    public boolean canPersonScanDataSources(Integer personId) {
        if (Objects.isNull(personId)) {
            return false;
        }

        var person = findOne(personId);
        return !Objects.isNull(person.getScopusAuthorId()) && !person.getScopusAuthorId().isEmpty();
    }

    private PersonIndex getPersonIndexForId(Integer personDatabaseId) {
        return personIndexRepository.findByDatabaseId(personDatabaseId).orElse(new PersonIndex());
    }

    private void setPersonIndexProperties(PersonIndex personIndex, Person savedPerson) {
        if (Objects.nonNull(savedPerson.getName().getOtherName())) {
            personIndex.setName(
                savedPerson.getName().getFirstname() + " " + savedPerson.getName().getOtherName() +
                    " " + savedPerson.getName().getLastname());
        } else {
            personIndex.setName(
                savedPerson.getName().getFirstname() + " " + savedPerson.getName().getLastname());
        }

        savedPerson.getOtherNames().forEach((otherName) -> {
            var fullName = Objects.requireNonNullElse(otherName.getFirstname(), "") + " " +
                Objects.requireNonNullElse(otherName.getOtherName(), "") + " " +
                Objects.requireNonNullElse(otherName.getLastname(), "");
            personIndex.setName(personIndex.getName() + "; " + fullName);
        });

        personIndex.setNameSortable(personIndex.getName());
        indexPersonBiography(personIndex, savedPerson);

        if (Objects.nonNull(savedPerson.getPersonalInfo().getLocalBirthDate())) {
            personIndex.setBirthdate(savedPerson.getPersonalInfo().getLocalBirthDate().toString());
        }
        personIndex.setBirthdateSortable(personIndex.getBirthdate());

        if (Objects.nonNull(savedPerson.getUser())) {
            personIndex.setUserId(savedPerson.getUser().getId());
        }

        personIndex.setDatabaseId(savedPerson.getId());
        personIndex.setOrcid(savedPerson.getOrcid());
        personIndex.setScopusAuthorId(savedPerson.getScopusAuthorId());
    }

    private void indexPersonBiography(PersonIndex personIndex, Person savedPerson) {
        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();
        multilingualContentService.buildLanguageStringsFromHTMLMC(srContent, otherContent,
            savedPerson.getBiography(), false);
        StringUtil.removeTrailingDelimiters(srContent, otherContent);
        personIndex.setBiographySr(
            !srContent.isEmpty() ? srContent.toString() : otherContent.toString());
        personIndex.setBiographyOther(
            !otherContent.isEmpty() ? otherContent.toString() : srContent.toString());
    }

    private void setPersonIndexEmploymentDetails(PersonIndex personIndex, Person savedPerson) {
        if (Objects.isNull(savedPerson.getInvolvements())) {
            return;
        }

        var employmentInstitutions = savedPerson.getInvolvements().stream()
            .filter(i -> (i.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                i.getInvolvementType().equals(InvolvementType.HIRED_BY)) &&
                Objects.isNull(i.getDateTo()))
            .map(Involvement::getOrganisationUnit).toList();

        personIndex.setPastEmploymentInstitutionIds(savedPerson.getInvolvements().stream()
            .filter(i -> (i.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                i.getInvolvementType().equals(InvolvementType.HIRED_BY)) &&
                Objects.nonNull(i.getDateTo()))
            .map(involvement -> involvement.getOrganisationUnit().getId()).toList());

        personIndex.setEmploymentInstitutionsId(
            employmentInstitutions.stream().map(BaseEntity::getId).collect(Collectors.toList()));

        var employmentsSr = new StringBuilder();
        var employmentsOther = new StringBuilder();
        for (var organisationUnit : employmentInstitutions) {
            var institutionNameSr = new StringBuilder();
            var institutionNameOther = new StringBuilder();

            organisationUnit.getName().stream()
                .filter(mc -> mc.getLanguage().getLanguageTag()
                    .startsWith(LanguageAbbreviations.SERBIAN))
                .forEach(mc -> institutionNameSr.append(mc.getContent()).append(" | "));
            organisationUnit.getName().stream()
                .filter(mc -> !mc.getLanguage().getLanguageTag()
                    .startsWith(LanguageAbbreviations.SERBIAN))
                .forEach(mc -> {
                    if (mc.getLanguage().getLanguageTag().equals(LanguageAbbreviations.ENGLISH)) {
                        institutionNameOther.insert(0, mc.getContent());
                    } else {
                        institutionNameOther.append(", ").append(mc.getContent());
                    }
                });

            employmentsSr.append(
                    institutionNameSr.toString().isEmpty() ? institutionNameOther : institutionNameSr)
                .append(organisationUnit.getNameAbbreviation()).append("; ");
            employmentsOther.append(institutionNameOther.toString().isEmpty() ?
                institutionNameSr.delete(institutionNameSr.length() - 3,
                    institutionNameSr.length()) :
                institutionNameOther).append("; ");
        }

        StringUtil.removeTrailingDelimiters(employmentsSr, employmentsOther);
        personIndex.setEmploymentsSr(
            !employmentsSr.isEmpty() ? employmentsSr.toString() : employmentsOther.toString());
        personIndex.setEmploymentsSrSortable(personIndex.getEmploymentsSr());
        personIndex.setEmploymentsOther(
            !employmentsOther.isEmpty() ? employmentsOther.toString() :
                employmentsSr.toString());
        personIndex.setEmploymentsOtherSortable(personIndex.getEmploymentsOther());
        setPersonIndexKeywords(personIndex, savedPerson);
    }

    private void setPersonIndexKeywords(PersonIndex personIndex, Person savedPerson) {
        var keywordsBuilder = new StringBuilder();
        savedPerson.getKeyword().forEach(multiLingualContent -> {
            keywordsBuilder.append(multiLingualContent.getContent()).append(", ");
        });
        personIndex.setKeywords(keywordsBuilder.toString());
    }

    @Override
    public Page<PersonIndex> findAllIndex(Pageable pageable) {
        return personIndexRepository.findAll(pageable);
    }

    @Override
    public Long getResearcherCount() {
        return personIndexRepository.count();
    }

    @Override
    public Page<PersonIndex> findPeopleByNameAndEmployment(List<String> tokens, Pageable pageable,
                                                           boolean strict) {
        return searchService.runQuery(buildNameAndEmploymentQuery(tokens, strict), pageable,
            PersonIndex.class, "person");
    }

    @Override
    public Page<PersonIndex> findPeopleForOrganisationUnit(Integer employmentInstitutionId,
                                                           Pageable pageable, Boolean fetchAlumni) {
        var ouHierarchyIds =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(employmentInstitutionId);

        if (fetchAlumni) {
            return personIndexRepository.findByPastEmploymentInstitutionIdsIn(pageable,
                ouHierarchyIds);
        }

        return personIndexRepository.findByEmploymentInstitutionsIdIn(pageable, ouHierarchyIds);
    }

    @Override
    public Page<PersonIndex> advancedSearch(List<String> tokens,
                                            Pageable pageable) {
        var query = expressionTransformer.parseAdvancedQuery(tokens);
        return searchService.runQuery(query, pageable, PersonIndex.class, "person");
    }

    @Override
    @Nullable
    public PersonIndex findPersonByScopusAuthorId(String scopusAuthorId) {
        return personIndexRepository.findByScopusAuthorId(scopusAuthorId).orElse(null);
    }

    private Query buildNameAndEmploymentQuery(List<String> tokens, boolean strict) {
        var minShouldMatch = (int) Math.ceil(tokens.size() * 0.8);

        return BoolQuery.of(q -> q
            .must(mb -> mb.bool(b -> {
                    tokens.forEach(
                        token -> {
                            if (token.startsWith("\\\"") && token.endsWith("\\\"")) {
                                b.must(mp ->
                                    mp.bool(m -> {
                                        {
                                            m.should(sb -> sb.matchPhrase(
                                                mq -> mq.field("employments_sr")
                                                    .query(token.replace("\\\"", ""))));
                                            m.should(sb -> sb.matchPhrase(
                                                mq -> mq.field("employments_other")
                                                    .query(token.replace("\\\"", ""))));
                                        }
                                        return m;
                                    }));
                            }

                            if (!strict) {
                                b.should(sb -> sb.wildcard(
                                    m -> m.field("name").value(token + "*").caseInsensitive(true)));
                            }

                            b.should(sb -> sb.match(m -> m.field("name").query(token)));
                            b.should(
                                sb -> sb.match(m -> m.field("employments_other").query(token)));
                            b.should(sb -> sb.match(m -> m.field("employments_sr").query(token)));
                            b.should(sb -> sb.match(m -> m.field("keywords").query(token)));
                        });
                    return b.minimumShouldMatch(Integer.toString(minShouldMatch));
                }
            ))
        )._toQuery();
    }

    private void setAllPersonIdentifiers(Person person, PersonIdentifierable personDTO) {
        IdentifierUtil.validateAndSetIdentifier(
            personDTO.getApvnt(),
            person.getId(),
            "^\\d+$",
            personRepository::existsByApvnt,
            person::setApvnt,
            "apvntFormatError",
            "apvntExistsError"
        );

        IdentifierUtil.validateAndSetIdentifier(
            personDTO.getECrisId(),
            person.getId(),
            "^\\d+$",
            personRepository::existsByeCrisId,
            person::setECrisId,
            "eCrisIdFormatError",
            "eCrisIdExistsError"
        );

        IdentifierUtil.validateAndSetIdentifier(
            personDTO.getENaukaId(),
            person.getId(),
            "^[A-Z]{2}\\d+$",
            personRepository::existsByeNaukaId,
            person::setENaukaId,
            "eNaukaIdFormatError",
            "eNaukaIdExistsError"
        );

        if (Objects.nonNull(personDTO.getOrcid()) &&
            personDTO.getOrcid().contains("https://orcid.org/")) {
            personDTO.setOrcid(personDTO.getOrcid().replace("https://orcid.org/", ""));
        }
        IdentifierUtil.validateAndSetIdentifier(
            personDTO.getOrcid(),
            person.getId(),
            "^\\d{4}-\\d{4}-\\d{4}-[\\dX]{4}$",
            personRepository::existsByOrcid,
            person::setOrcid,
            "orcidIdFormatError",
            "orcidIdExistsError"
        );

        IdentifierUtil.validateAndSetIdentifier(
            personDTO.getScopusAuthorId(),
            person.getId(),
            "^\\d+$",
            personRepository::existsByScopusAuthorId,
            person::setScopusAuthorId,
            "scopusAuthorIdFormatError",
            "scopusAuthorIdExistsError"
        );
    }
}
