package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.FlexibleDateDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.JournalPublicationType;
import rs.teslaris.core.model.document.PerformanceRelatedOutputType;
import rs.teslaris.core.model.document.ProceedingsPublicationType;
import rs.teslaris.core.model.document.PublicationStatus;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * Converters for the curriculum output kinds this PoC supports.
 * <p>
 * Contributions link the curriculum owner and co-authors with an already migrated Ciência ID to
 * their persons; everyone else is kept name-only.
 */
@Component
@RequiredArgsConstructor
public class HydratorOutputConverters {

    private static final Pattern ET_AL = Pattern.compile("et\\.?\\s*al\\.?",
        Pattern.CASE_INSENSITIVE);

    // relationship-type code of a container's identifier (journal ISSN, book ISBN)
    private static final String PART_OF = "PD";

    private static final String ISSN_CODE = "issn";

    private static final String ISBN_CODE = "isbn";

    private static final String HANDLE_CODE = "handle";

    // Internal id of the source system, not a real PID - expected on almost every output
    private static final String SOURCE_WORK_ID_CODE = "source-work-id";

    private static final String NOT_INFORMED = "Not informed";

    /**
     * CW-001-MAP-000067: identifier types of the work itself, the document field each one fills,
     * and the format core accepts for it. Core rejects the whole document on a malformed
     * identifier, so a value failing the format is dropped here instead. The patterns mirror {@code
     * DocumentPublicationServiceImpl#setCommonIdentifiers}; keep them in sync.
     */
    private static final Map<String, IdentifierTarget> IDENTIFIER_TARGETS = Map.of(
        "doi", new IdentifierTarget(DocumentDTO::setDoi,
            "^10\\.\\d{4,9}\\/[-,._;()/:A-Z0-9]+$"),
        "eid", new IdentifierTarget(DocumentDTO::setScopusId, "^\\d{6,12}$"),
        "wosuid", new IdentifierTarget(DocumentDTO::setWebOfScienceId, "\\d{15}$"),
        "pmid", new IdentifierTarget(DocumentDTO::setPubmedId, "^(?:\\d{1,8})$"),
        "arxiv", new IdentifierTarget(DocumentDTO::setArxivId,
            "^(?:\\d{4}\\.\\d{4,5}|[a-z\\-]+/\\d{7})$"),
        "ssrn", new IdentifierTarget(DocumentDTO::setSsrnId, "^(?:\\d+)$"),
        "handle", new IdentifierTarget(DocumentDTO::setHandleId,
            "^(?:\\d{2}\\.\\d{3,}\\.\\d+/\\S+)$")
    );

    // Prefixes the source keeps on an id that core stores bare (Scopus EID, WoS UID)
    private static final Map<String, Pattern> IDENTIFIER_PREFIXES = Map.of(
        "eid", Pattern.compile("^2-s2\\.0-", Pattern.CASE_INSENSITIVE),
        "wosuid", Pattern.compile("^(?:WOS|ISI):", Pattern.CASE_INSENSITIVE)
    );

    private static final Map<String, PublicationStatus> PUBLICATION_STATUSES = Map.of(
        "PBD", PublicationStatus.PUBLISHED,
        "APB", PublicationStatus.ACCEPTED,
        "SBT", PublicationStatus.SUBMITTED,
        "NPR", PublicationStatus.IN_PRINT,
        "ERV", PublicationStatus.IN_REVIEW
    );

    /**
     * CW-001-MAP-000065: authoring / editing role codes of the curriculum owner.
     */
    private static final Map<String, DocumentContributionType> OWNER_ROLES = Map.of(
        "AT", DocumentContributionType.AUTHOR,
        "CA", DocumentContributionType.AUTHOR,
        "ED", DocumentContributionType.EDITOR,
        "CE", DocumentContributionType.EDITOR,
        "EC", DocumentContributionType.INVITED_EDITOR,
        "EA", DocumentContributionType.ASSOCIATED_EDITOR,
        "TD", DocumentContributionType.TRANSLATOR,
        "PS", DocumentContributionType.ASSISTANT_STAFF
    );

    // Every output type has its own CIÊNCIA VITAE rule for the same source field
    private static final Map<MigrationEntityType, OutputRules> OUTPUT_RULES = Map.of(
        MigrationEntityType.JOURNAL_PUBLICATION, new OutputRules(62, 67, 68, 69),
        MigrationEntityType.PROCEEDINGS_PUBLICATION, new OutputRules(76, 85, 86, 88),
        // Slides carry only the presentation date and the presenters
        MigrationEntityType.PERFORMANCE_RELATED_OUTPUT, new OutputRules(90, 90, 91, 90),
        MigrationEntityType.THESIS, new OutputRules(212, 214, 215, 217)
    );

    private final HydratorConversionUtil conversionUtil;

    private final HydratorJournalResolver journalResolver;

    private final HydratorPersonResolver personResolver;

    private final CountryService countryService;

    private final MigrationLog migrationLog;


    public JournalPublicationDTO toJournalPublication(HydratorCVModel.Curriculum record,
                                                      HydratorCVModel.Output output) {
        var article = output.journalArticle();

        if (Objects.isNull(article) || isBlank(article.articleTitle())) {
            return null;
        }

        var language = languageOf(record);
        var dto = new JournalPublicationDTO();

        applyCommonFields(dto, record, output, MigrationEntityType.JOURNAL_PUBLICATION,
            article.articleTitle(), language, article.publicationDate(), article.url(),
            article.authors(), article.authoringRole());
        applyIdentifiers(dto, record, output, MigrationEntityType.JOURNAL_PUBLICATION,
            article.identifiers(), null);
        applyPublicationFields(dto, record, output, MigrationEntityType.JOURNAL_PUBLICATION,
            language, article.keywords(), article.publicationStatus(),
            article.researchClassifications());
        dto.setPeerReviewed(isTrue(article.refereed()));
        dto.setOpenAccess(isTrue(article.openAccess()));
        applyLocation(dto, language, article.publicationLocation());

        dto.setJournalPublicationType(JournalPublicationType.RESEARCH_ARTICLE);
        dto.setVolume(article.volume());
        dto.setIssue(article.issue());
        dto.setStartPage(article.pageFrom());
        dto.setEndPage(article.pageTo());

        return dto;
    }

    // Called on creation, so skipped or filtered articles create no journal stubs
    Integer journalId(HydratorCVModel.Curriculum record, HydratorCVModel.Output output) {
        var article = output.journalArticle();

        return journalResolver.resolveOrCreate(article.journal(),
            partOfIdentifier(article.identifiers(), ISSN_CODE), languageOf(record));
    }

    /**
     * CW-001-MAP-000071: the paper only; its proceedings and conference are resolved when it is
     * created (see {@link HydratorProceedingsResolver}), as core overwrites the paper's date and
     * status from the proceedings anyway.
     */
    public ProceedingsPublicationDTO toProceedingsPublication(HydratorCVModel.Curriculum record,
                                                              HydratorCVModel.Output output) {
        var paper = output.conferencePaper();

        if (Objects.isNull(paper) || isBlank(paper.paperTitle())) {
            return null;
        }

        var language = languageOf(record);
        var dto = new ProceedingsPublicationDTO();
        var entityType = MigrationEntityType.PROCEEDINGS_PUBLICATION;

        applyCommonFields(dto, record, output, entityType, paper.paperTitle(), language,
            paper.conferenceDate(), null, paper.authors(), paper.authoringRole());
        applyIdentifiers(dto, record, output, entityType, paper.identifiers(), ISBN_CODE);
        applyPublicationFields(dto, record, output, entityType, language, paper.keywords(),
            paper.publicationStatus(), paper.researchClassifications());

        dto.setProceedingsPublicationType(ProceedingsPublicationType.REGULAR_FULL_ARTICLE);
        dto.setStartPage(paper.pageFrom());
        dto.setEndPage(paper.pageTo());

        if (!isBlank(paper.proceedingsPublisher())) {
            migrationLog.valueDropped(HydratorSource.NAME, entityType.name(),
                outputKey(record, output), HydratorMappingRule.cv(83),
                "proceedings publisher not migrated: '" + paper.proceedingsPublisher().trim() +
                    "'");
        }

        return dto;
    }

    /**
     * CW-001-MAP-000090/091: the paper's presentation as slides, held at a performance event of its
     * own (see {@link HydratorProceedingsResolver#createPresentationEvent}). TeslaRIS has no
     * TOPIC_OF document relation, so the slides are not linked to the paper.
     */
    public PerformanceRelatedOutputDTO toPresentation(HydratorCVModel.Curriculum record,
                                                      HydratorCVModel.Output output) {
        var paper = output.conferencePaper();

        if (Objects.isNull(paper) || isBlank(paper.paperTitle()) ||
            (Objects.isNull(conversionUtil.flexibleDate(paper.presentationDate())) &&
                !hasAuthors(paper.presenters()))) {
            return null;
        }

        var dto = new PerformanceRelatedOutputDTO();
        var entityType = MigrationEntityType.PERFORMANCE_RELATED_OUTPUT;

        applyCommonFields(dto, record, output, entityType, paper.paperTitle(), languageOf(record),
            presentationDate(paper), null, null, null);
        dto.setType(PerformanceRelatedOutputType.SLIDES);
        dto.setContributions(linkedContributions(record, paper.presenters(),
            DocumentContributionType.PRESENTER));
        dto.getTitle().stream()
            .map(MultilingualContentDTO::getLanguageTagId)
            .filter(Objects::nonNull)
            .forEach(dto.getLanguageTagIds()::add);

        migrationLog.valueDropped(HydratorSource.NAME, entityType.name(),
            outputKey(record, output), HydratorMappingRule.cv(90),
            "no TOPIC_OF relation in TeslaRIS, slides are not linked to the paper");

        return dto;
    }

    // The presentation date, or the conference's when only the presenters are known
    HydratorCVModel.DateInfo presentationDate(HydratorCVModel.ConferencePaper paper) {
        return Objects.isNull(conversionUtil.flexibleDate(paper.presentationDate())) ?
            paper.conferenceDate() : paper.presentationDate();
    }

    public ThesisDTO toThesis(HydratorCVModel.Curriculum record, HydratorCVModel.Output output) {
        var dissertation = output.dissertation();

        if (Objects.isNull(dissertation) || isBlank(dissertation.title())) {
            return null;
        }

        var language = languageOf(record);
        var dto = new ThesisDTO();
        var entityType = MigrationEntityType.THESIS;

        applyCommonFields(dto, record, output, entityType, dissertation.title(),
            language, dissertation.completionDate(), dissertation.url(),
            dissertation.authors(), null);
        applyIdentifiers(dto, record, output, entityType,
            dissertation.identifiers(), null);
        applyPublicationFields(dto, record, output, entityType, language, dissertation.keywords(),
            null, dissertation.researchClassifications());

        dto.setThesisType(thesisType(dissertation.degreeType()));
        dto.setExternalOrganisationUnitName(conversionUtil.multilingualContent(
            institutionName(record, output, dissertation.institutions()), language));
        dto.getContributions().addAll(supervisorContributions(dissertation.supervisors()));

        if (!isBlank(dissertation.numberOfVolumes())) {
            dto.setDescription(conversionUtil.multilingualContent(
                "Number of volumes: " + dissertation.numberOfVolumes().trim(), "en"));
        }

        dropValue(record, output, entityType, HydratorMappingRule.cv(211),
            dissertation.classification(),
            "classification vocabulary is not defined");

        return dto;
    }

    // CW-001-MAP-000209: a thesis has one institution, kept by name until matched to an
    // organisation unit; core rejects a thesis without one
    private String institutionName(HydratorCVModel.Curriculum record,
                                   HydratorCVModel.Output output,
                                   HydratorCVModel.Institutions institutions) {
        var names = Objects.isNull(institutions) || Objects.isNull(institutions.institution()) ?
            List.<String>of() : institutions.institution().stream()
            .filter(Objects::nonNull)
            .map(HydratorCVModel.Institution::name)
            .filter(name -> !isBlank(name))
            .map(String::trim)
            .toList();

        if (names.isEmpty()) {
            return NOT_INFORMED;
        }

        names.stream().skip(1).forEach(name -> migrationLog.valueDropped(HydratorSource.NAME,
            MigrationEntityType.THESIS.name(), outputKey(record, output),
            HydratorMappingRule.cv(209),
            "only one institution is kept: '" + name + "'"));

        return names.getFirst();
    }

    // CW-001-MAP-000216: supervisors and co-supervisors alike, after the author
    private List<PersonDocumentContributionDTO> supervisorContributions(
        HydratorCVModel.Supervisors supervisors) {
        var contributions = new ArrayList<PersonDocumentContributionDTO>();

        if (Objects.isNull(supervisors) || Objects.isNull(supervisors.supervisor())) {
            return contributions;
        }

        for (var supervisor : supervisors.supervisor()) {
            var name = Objects.isNull(supervisor) ? null :
                conversionUtil.splitName(supervisor.name(), null);

            if (Objects.nonNull(name)) {
                contributions.add(contribution(DocumentContributionType.ADVISOR,
                    contributions.size() + 2, name,
                    personResolver.resolveByCienciaId(supervisor.cienciaId())));
            }
        }

        return contributions;
    }

    private void dropValue(HydratorCVModel.Curriculum record, HydratorCVModel.Output output,
                           MigrationEntityType entityType, String rule, String value,
                           String reason) {
        if (!isBlank(value)) {
            migrationLog.valueDropped(HydratorSource.NAME, entityType.name(),
                outputKey(record, output), rule, reason + ": '" + value.trim() + "'");
        }
    }

    private void applyCommonFields(DocumentDTO dto, HydratorCVModel.Curriculum record,
                                   HydratorCVModel.Output output, MigrationEntityType entityType,
                                   String title, String language, HydratorCVModel.DateInfo date,
                                   String url, HydratorCVModel.OutputAuthors authors,
                                   HydratorCVModel.CodeValue ownerRole) {
        dto.setTitle(conversionUtil.multilingualContent(title, language));
        dto.setSubTitle(List.of());
        dto.setDescription(List.of());
        dto.setKeywords(List.of());
        dto.setUris(new HashSet<>());
        dto.setDocumentDate(documentDate(record, output, entityType, date));
        dto.setContributions(contributions(record, output, entityType, authors, ownerRole));

        // CW-001-MAP-000972 asks for a generated citation as the fallback description. The source's
        // output:citation is only the author list, so the description is left empty for now.

        if (!isBlank(url)) {
            dto.getUris().add(url.trim());
        }
    }

    private FlexibleDateDTO documentDate(HydratorCVModel.Curriculum record,
                                         HydratorCVModel.Output output,
                                         MigrationEntityType entityType,
                                         HydratorCVModel.DateInfo date) {
        var documentDate = conversionUtil.flexibleDate(date);

        if (Objects.nonNull(documentDate) &&
            ((!isBlank(date.month()) && Objects.isNull(documentDate.month())) ||
                (!isBlank(date.day()) && Objects.isNull(documentDate.day())))) {
            migrationLog.valueDropped(HydratorSource.NAME, entityType.name(),
                outputKey(record, output),
                HydratorMappingRule.cv(OUTPUT_RULES.get(entityType).date()),
                String.format("invalid date part (year='%s', month='%s', day='%s')",
                    date.year(), date.month(), date.day()));
        }

        return documentDate;
    }

    /**
     * Fields the published output types share: keywords and status. Research classifications
     * are only logged under the type's own rule.
     */
    private void applyPublicationFields(DocumentDTO dto, HydratorCVModel.Curriculum record,
                                        HydratorCVModel.Output output,
                                        MigrationEntityType entityType, String language,
                                        HydratorCVModel.Keywords keywords,
                                        HydratorCVModel.CodeValue status,
                                        HydratorCVModel.ResearchClassifications classifications) {
        dto.setKeywords(conversionUtil.multilingualContent(keywords(keywords), language));
        dto.setPublicationStatus(publicationStatus(status));
        dropResearchClassifications(record, output, entityType, classifications);
    }

    void applyLocation(DocumentDTO dto, String language,
                       HydratorCVModel.PublicationLocation location) {
        if (Objects.isNull(location)) {
            return;
        }

        if (!isBlank(location.city())) {
            dto.setCity(conversionUtil.multilingualContent(location.city(), language));
        }

        if (Objects.nonNull(location.country()) && !isBlank(location.country().code())) {
            countryService.findCountryByCode(location.country().code().trim())
                .ifPresent(country -> dto.setCountryId(country.getId()));
        }
    }

    // Documents have no research areas in TeslaRIS, so the values are only logged
    private void dropResearchClassifications(HydratorCVModel.Curriculum record,
                                             HydratorCVModel.Output output,
                                             MigrationEntityType entityType,
                                             HydratorCVModel.ResearchClassifications
                                                 classifications) {
        if (Objects.isNull(classifications) ||
            Objects.isNull(classifications.researchClassification())) {
            return;
        }

        classifications.researchClassification().stream()
            .filter(Objects::nonNull)
            .forEach(classification -> migrationLog.valueDropped(HydratorSource.NAME,
                entityType.name(), outputKey(record, output),
                HydratorMappingRule.cv(OUTPUT_RULES.get(entityType).researchClassifications()),
                "document has no research areas: '" + classification.value() + "'"));
    }

    /**
     * Identifiers of the work itself fill their document fields, first value wins. "Part of"
     * identifiers, and any of the container's type, belong to the container and are join keys,
     * not attributes.
     */
    private void applyIdentifiers(DocumentDTO dto, HydratorCVModel.Curriculum record,
                                  HydratorCVModel.Output output, MigrationEntityType entityType,
                                  HydratorCVModel.OutputIdentifiers identifiers,
                                  String containerCode) {
        if (Objects.isNull(identifiers) || Objects.isNull(identifiers.identifier())) {
            return;
        }

        var filled = new HashSet<String>();

        for (var identifier : identifiers.identifier()) {
            var code = identifierCode(identifier);

            if (Objects.isNull(code) || isBlank(identifier.identifier()) ||
                isPartOf(identifier) || SOURCE_WORK_ID_CODE.equals(code) ||
                code.equals(containerCode)) {
                continue;
            }

            var target = IDENTIFIER_TARGETS.get(code);

            if (Objects.isNull(target)) {
                migrationLog.valueDropped(HydratorSource.NAME, entityType.name(),
                    outputKey(record, output),
                    HydratorMappingRule.cv(OUTPUT_RULES.get(entityType).identifiers()),
                    "unsupported identifier type '" + code + "'");
                continue;
            }

            var value = withoutPrefix(code, identifier.identifier().trim());

            if (!target.pattern().matcher(value).matches()) {
                var handleUri = HANDLE_CODE.equals(code) ? handleUri(value) : null;

                if (Objects.nonNull(handleUri)) {
                    dto.getUris().add(handleUri);
                    continue;
                }

                migrationLog.valueDropped(HydratorSource.NAME, entityType.name(),
                    outputKey(record, output),
                    HydratorMappingRule.cv(OUTPUT_RULES.get(entityType).identifiers()),
                    "invalid " + code + " format: '" + value + "'");
                continue;
            }

            if (filled.add(code)) {
                target.setter().accept(dto, value);
            }
        }
    }

    // Core's handle field only takes "20.500.x/…", so any other handle is kept as a link
    private String handleUri(String value) {
        if (value.matches("(?i)^https?://\\S+$")) {
            return value;
        }

        return value.matches("^\\d+(\\.\\d+)*/\\S+$") ? "https://hdl.handle.net/" + value : null;
    }

    private String withoutPrefix(String code, String value) {
        var prefix = IDENTIFIER_PREFIXES.get(code);

        return Objects.isNull(prefix) ? value : prefix.matcher(value).replaceFirst("");
    }

    String partOfIdentifier(HydratorCVModel.OutputIdentifiers identifiers, String code) {
        return firstIdentifier(identifiers, code, this::isPartOf);
    }

    // CW-001-MAP-000085: a paper has no ISBN of its own, so one marked "Self" is its proceedings'
    String proceedingsIsbn(HydratorCVModel.OutputIdentifiers identifiers) {
        return Optional.ofNullable(partOfIdentifier(identifiers, ISBN_CODE))
            .orElseGet(() -> firstIdentifier(identifiers, ISBN_CODE, identifier -> true));
    }

    private String firstIdentifier(HydratorCVModel.OutputIdentifiers identifiers, String code,
                                   Predicate<HydratorCVModel.OutputIdentifier> filter) {
        if (Objects.isNull(identifiers) || Objects.isNull(identifiers.identifier())) {
            return null;
        }

        return identifiers.identifier().stream()
            .filter(filter)
            .filter(identifier -> code.equals(identifierCode(identifier)))
            .map(HydratorCVModel.OutputIdentifier::identifier)
            .filter(value -> !isBlank(value))
            .findFirst()
            .orElse(null);
    }

    private boolean isPartOf(HydratorCVModel.OutputIdentifier identifier) {
        return Objects.nonNull(identifier.relationshipType()) &&
            PART_OF.equalsIgnoreCase(identifier.relationshipType().code());
    }

    private String identifierCode(HydratorCVModel.OutputIdentifier identifier) {
        if (Objects.isNull(identifier.identifierType()) ||
            isBlank(identifier.identifierType().code())) {
            return null;
        }

        return identifier.identifierType().code().trim().toLowerCase();
    }

    private List<PersonDocumentContributionDTO> contributions(HydratorCVModel.Curriculum record,
                                                              HydratorCVModel.Output output,
                                                              MigrationEntityType entityType,
                                                              HydratorCVModel.OutputAuthors authors,
                                                              HydratorCVModel.CodeValue ownerRole) {
        var contributions = new ArrayList<PersonDocumentContributionDTO>();

        if (Objects.isNull(authors) || Objects.isNull(authors.author())) {
            return contributions;
        }

        var people = new ArrayList<HydratorCVModel.OutputAuthor>();
        var names = new ArrayList<PersonNameDTO>();

        for (var author : authors.author()) {
            var personName = conversionUtil.splitName(author.name(), null);

            if (Objects.nonNull(personName) && !isEtAl(author.name())) {
                people.add(author);
                names.add(personName);
            }
        }

        var ownerIndex = ownerIndex(record, output, entityType, people, names);

        for (int i = 0; i < people.size(); i++) {
            var isOwner = i == ownerIndex;

            contributions.add(contribution(
                isOwner ? ownerContributionType(ownerRole) : DocumentContributionType.AUTHOR,
                i + 1, names.get(i), isOwner ? personResolver.resolveOwner(record) :
                    personResolver.resolveByCienciaId(people.get(i).cienciaId())));
        }

        return contributions;
    }

    // CW-001-MAP-000087: proceedings editors
    List<PersonDocumentContributionDTO> editorContributions(HydratorCVModel.Curriculum record,
                                                            HydratorCVModel.OutputAuthors editors) {
        return linkedContributions(record, editors, DocumentContributionType.EDITOR);
    }

    // People of one role, linked as the owner when marked "self", otherwise by Ciência ID only
    private List<PersonDocumentContributionDTO> linkedContributions(
        HydratorCVModel.Curriculum record, HydratorCVModel.OutputAuthors people,
        DocumentContributionType type) {
        var contributions = new ArrayList<PersonDocumentContributionDTO>();

        if (!hasAuthors(people)) {
            return contributions;
        }

        for (var person : people.author()) {
            var personName = conversionUtil.splitName(person.name(), null);

            if (Objects.nonNull(personName) && !isEtAl(person.name())) {
                contributions.add(contribution(type, contributions.size() + 1, personName,
                    Boolean.TRUE.equals(person.self()) ? personResolver.resolveOwner(record) :
                        personResolver.resolveByCienciaId(person.cienciaId())));
            }
        }

        return contributions;
    }

    private PersonDocumentContributionDTO contribution(DocumentContributionType type,
                                                       int orderNumber, PersonNameDTO name,
                                                       Integer personId) {
        var contribution = new PersonDocumentContributionDTO();
        contribution.setContributionType(type);
        contribution.setIsMainContributor(false);
        contribution.setIsCorrespondingContributor(false);
        contribution.setIsBoardPresident(false);
        contribution.setOrderNumber(orderNumber);
        contribution.setContributionDescription(List.of());
        contribution.setDisplayAffiliationStatement(List.of());
        contribution.setInstitutionIds(List.of());
        contribution.setPersonName(name);
        contribution.setPersonId(personId);

        return contribution;
    }

    private boolean hasAuthors(HydratorCVModel.OutputAuthors authors) {
        return Objects.nonNull(authors) && Objects.nonNull(authors.author()) &&
            !authors.author().isEmpty();
    }

    /**
     * Which author is the curriculum owner. The source does not always mark them with
     * {@code self}, so the owner's Ciência ID and then their name forms are tried as well, read
     * surname first only when nothing else matches.
     *
     * @return the owner's position in {@code people}, or -1 when they cannot be told apart
     */
    private int ownerIndex(HydratorCVModel.Curriculum record, HydratorCVModel.Output output,
                           MigrationEntityType entityType,
                           List<HydratorCVModel.OutputAuthor> people, List<PersonNameDTO> names) {
        for (int i = 0; i < people.size(); i++) {
            if (Boolean.TRUE.equals(people.get(i).self())) {
                return i;
            }
        }

        var ownerCienciaId = personResolver.ownerCienciaId(record);

        if (Objects.nonNull(ownerCienciaId)) {
            for (int i = 0; i < people.size(); i++) {
                if (ownerCienciaId.equalsIgnoreCase(
                    Objects.toString(people.get(i).cienciaId(), "").trim())) {
                    return i;
                }
            }
        }

        var match = nameMatch(names, personResolver.ownerNames(record));

        if (match == -1) {
            match = nameMatch(names, personResolver.ownerSurnameFirstNames(record));
        }

        if (match < 0 && !people.isEmpty()) {
            migrationLog.valueDropped(HydratorSource.NAME, entityType.name(),
                outputKey(record, output),
                HydratorMappingRule.cv(OUTPUT_RULES.get(entityType).authors()),
                (match == -2 ? "several authors match the owner's name" :
                    "owner not identified") + " among " + people.size() + " authors");
            return -1;
        }

        return match;
    }

    /**
     * The author whose surname and first initial match one of the owner's names. Authors sharing
     * those are told apart by the full first given name; one author entered twice is the first.
     *
     * @return the author's position, -1 when none matches, -2 when several remain
     */
    private int nameMatch(List<PersonNameDTO> names, List<PersonNameDTO> ownerNames) {
        var ownerKeys = ownerNames.stream()
            .map(conversionUtil::authorMatchKey)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        var candidates = IntStream.range(0, names.size())
            .filter(i -> ownerKeys.contains(conversionUtil.authorMatchKey(names.get(i))))
            .boxed()
            .toList();

        if (candidates.size() <= 1) {
            return candidates.isEmpty() ? -1 : candidates.getFirst();
        }

        var ownerGivenNames = ownerNames.stream()
            .map(conversionUtil::firstGivenName)
            .filter(givenName -> givenName.length() > 1)
            .collect(Collectors.toSet());
        var byGivenName = candidates.stream()
            .filter(i -> ownerGivenNames.contains(conversionUtil.firstGivenName(names.get(i))))
            .toList();

        if (byGivenName.size() == 1) {
            return byGivenName.getFirst();
        }

        var isRepeated = candidates.stream()
            .map(i -> conversionUtil.fullNameKey(names.get(i)))
            .distinct()
            .count() == 1;

        return isRepeated ? candidates.getFirst() : -2;
    }

    // Only the owner's role is known
    private DocumentContributionType ownerContributionType(HydratorCVModel.CodeValue role) {
        if (Objects.isNull(role) || isBlank(role.code())) {
            return DocumentContributionType.AUTHOR;
        }

        return OWNER_ROLES.getOrDefault(role.code().trim().toUpperCase(),
            DocumentContributionType.AUTHOR);
    }

    // "et al" closes truncated author lists in the source; it is not a person
    private boolean isEtAl(String name) {
        return ET_AL.matcher(name.trim()).matches();
    }

    private PublicationStatus publicationStatus(HydratorCVModel.CodeValue status) {
        if (Objects.isNull(status) || isBlank(status.code())) {
            return null;
        }

        return PUBLICATION_STATUSES.get(status.code().trim().toUpperCase());
    }

    // CW-001-MAP-000070: one keyword per line, as everywhere else in TeslaRIS
    private String keywords(HydratorCVModel.Keywords keywords) {
        if (Objects.isNull(keywords) || Objects.isNull(keywords.keyword())) {
            return null;
        }

        return String.join("\n", keywords.keyword().stream()
            .filter(keyword -> !isBlank(keyword))
            .map(String::trim)
            .distinct()
            .toList());
    }

    private ThesisType thesisType(HydratorCVModel.DegreeType degreeType) {
        if (Objects.isNull(degreeType)) {
            return ThesisType.PHD;
        }

        var value = Objects.toString(degreeType.value(), "").toLowerCase();

        if (value.contains("master")) {
            return ThesisType.MASTER;
        }

        if (value.contains("magist")) {
            return ThesisType.MR;
        }

        // "Degree" is the Portuguese licenciatura
        if (value.contains("degree")) {
            return ThesisType.BACHELOR;
        }

        return ThesisType.PHD;
    }

    /**
     * Output ids are unique within a curriculum only, so the key is composite. The same paper
     * listed in several co-authors' curricula therefore produces different keys - cross-curriculum
     * duplicates are caught by the duplicate failure handler, not by the record log.
     */
    String outputKey(HydratorCVModel.Curriculum record, HydratorCVModel.Output output) {
        return record.id() + "#output#" + Objects.toString(output.id(), "unknown");
    }

    String languageOf(HydratorCVModel.Curriculum record) {
        return Objects.isNull(record.curriculum()) ? null : record.curriculum().language();
    }

    private boolean isTrue(String value) {
        return "true".equalsIgnoreCase(Objects.toString(value, "").trim());
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }

    private record OutputRules(int date, int identifiers, int authors,
                               int researchClassifications) {
    }

    private record IdentifierTarget(BiConsumer<DocumentDTO, String> setter, Pattern pattern) {

        IdentifierTarget(BiConsumer<DocumentDTO, String> setter, String regex) {
            // Core matches case-insensitively as well
            this(setter, Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
        }
    }
}
