package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.FlexibleDateDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.JournalPublicationType;
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

    // Internal id of the source system, not a real PID - expected on almost every output
    private static final String SOURCE_WORK_ID_CODE = "source-work-id";

    /**
     * MAP-000067: identifier types of the work itself, the document field each one fills, and the
     * format core accepts for it. Core rejects the whole document on a malformed identifier, so a
     * value failing the format is dropped here instead. The patterns mirror
     * {@code DocumentPublicationServiceImpl#setCommonIdentifiers}; keep them in sync.
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

    private static final Map<String, PublicationStatus> PUBLICATION_STATUSES = Map.of(
        "PBD", PublicationStatus.PUBLISHED,
        "APB", PublicationStatus.ACCEPTED,
        "SBT", PublicationStatus.SUBMITTED,
        "NPR", PublicationStatus.IN_PRINT,
        "ERV", PublicationStatus.IN_REVIEW
    );

    /**
     * MAP-000065: authoring / editing role codes of the curriculum owner.
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
            article.identifiers(), article.authors(), article.authoringRole());
        applyPublicationFields(dto, record, output, MigrationEntityType.JOURNAL_PUBLICATION,
            language, article);

        dto.setJournalPublicationType(JournalPublicationType.RESEARCH_ARTICLE);
        dto.setVolume(article.volume());
        dto.setIssue(article.issue());
        dto.setStartPage(article.pageFrom());
        dto.setEndPage(article.pageTo());
        dto.setJournalId(journalResolver.resolveOrCreate(article.journal(),
            partOfIdentifier(article.identifiers(), ISSN_CODE), language));

        return dto;
    }

    public ThesisDTO toThesis(HydratorCVModel.Curriculum record, HydratorCVModel.Output output) {
        var dissertation = output.dissertation();

        if (Objects.isNull(dissertation) || isBlank(dissertation.title())) {
            return null;
        }

        var language = languageOf(record);
        var dto = new ThesisDTO();

        applyCommonFields(dto, record, output, MigrationEntityType.THESIS, dissertation.title(),
            language, dissertation.completionDate(), dissertation.url(),
            dissertation.identifiers(), dissertation.authors(), null);

        dto.setThesisType(thesisType(dissertation.degreeType()));

        return dto;
    }

    private void applyCommonFields(DocumentDTO dto, HydratorCVModel.Curriculum record,
                                   HydratorCVModel.Output output, MigrationEntityType entityType,
                                   String title, String language, HydratorCVModel.DateInfo date,
                                   String url, HydratorCVModel.OutputIdentifiers identifiers,
                                   HydratorCVModel.OutputAuthors authors,
                                   HydratorCVModel.CodeValue ownerRole) {
        dto.setTitle(conversionUtil.multilingualContent(title, language));
        dto.setSubTitle(List.of());
        dto.setDescription(List.of());
        dto.setKeywords(List.of());
        dto.setUris(new HashSet<>());
        dto.setDocumentDate(documentDate(record, output, entityType, date));
        dto.setContributions(contributions(record, output, entityType, authors, ownerRole));
        applyIdentifiers(dto, record, output, entityType, identifiers);

        // MAP-000972 asks for a generated citation as the fallback description. The source's
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
                outputKey(record, output), "date",
                String.format("invalid date part (year='%s', month='%s', day='%s')",
                    date.year(), date.month(), date.day()));
        }

        return documentDate;
    }

    /**
     * Fields the periodical-like output types share: keywords, status, place of publication,
     * peer review and open access.
     */
    private void applyPublicationFields(DocumentDTO dto, HydratorCVModel.Curriculum record,
                                        HydratorCVModel.Output output,
                                        MigrationEntityType entityType, String language,
                                        HydratorCVModel.JournalArticle article) {
        dto.setKeywords(conversionUtil.multilingualContent(keywords(article.keywords()),
            language));
        dto.setPublicationStatus(publicationStatus(article.publicationStatus()));
        dto.setPeerReviewed(isTrue(article.refereed()));
        dto.setOpenAccess(isTrue(article.openAccess()));
        applyLocation(dto, language, article.publicationLocation());
        dropResearchClassifications(record, output, entityType,
            article.researchClassifications());
    }

    private void applyLocation(DocumentDTO dto, String language,
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

    // MAP-000069: documents have no research areas in TeslaRIS, so the values are only logged
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
                entityType.name(), outputKey(record, output), "MAP-000069",
                "document has no research areas: '" + classification.value() + "'"));
    }

    /**
     * MAP-000067: identifiers of the work itself fill their document fields, first value wins.
     * "Part of" identifiers belong to the container and are join keys, not attributes.
     */
    private void applyIdentifiers(DocumentDTO dto, HydratorCVModel.Curriculum record,
                                  HydratorCVModel.Output output, MigrationEntityType entityType,
                                  HydratorCVModel.OutputIdentifiers identifiers) {
        if (Objects.isNull(identifiers) || Objects.isNull(identifiers.identifier())) {
            return;
        }

        var filled = new HashSet<String>();

        for (var identifier : identifiers.identifier()) {
            var code = identifierCode(identifier);

            if (Objects.isNull(code) || isBlank(identifier.identifier()) ||
                isPartOf(identifier) || SOURCE_WORK_ID_CODE.equals(code)) {
                continue;
            }

            var target = IDENTIFIER_TARGETS.get(code);

            if (Objects.isNull(target)) {
                migrationLog.valueDropped(HydratorSource.NAME, entityType.name(),
                    outputKey(record, output), "MAP-000067",
                    "unsupported identifier type '" + code + "'");
                continue;
            }

            var value = identifier.identifier().trim();

            if (!target.pattern().matcher(value).matches()) {
                migrationLog.valueDropped(HydratorSource.NAME, entityType.name(),
                    outputKey(record, output), "MAP-000067",
                    "invalid " + code + " format: '" + value + "'");
                continue;
            }

            if (filled.add(code)) {
                target.setter().accept(dto, value);
            }
        }
    }

    private String partOfIdentifier(HydratorCVModel.OutputIdentifiers identifiers, String code) {
        if (Objects.isNull(identifiers) || Objects.isNull(identifiers.identifier())) {
            return null;
        }

        return identifiers.identifier().stream()
            .filter(this::isPartOf)
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

        return identifier.identifierType().code().trim().toLowerCase(Locale.ROOT);
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
            var orderNumber = i + 1;

            var contribution = new PersonDocumentContributionDTO();
            contribution.setContributionType(
                isOwner ? ownerContributionType(ownerRole) : DocumentContributionType.AUTHOR);
            contribution.setIsMainContributor(orderNumber == 1);
            contribution.setIsCorrespondingContributor(false);
            contribution.setIsBoardPresident(false);
            contribution.setOrderNumber(orderNumber);
            contribution.setContributionDescription(List.of());
            contribution.setDisplayAffiliationStatement(List.of());
            contribution.setInstitutionIds(List.of());
            contribution.setPersonName(names.get(i));
            contribution.setPersonId(isOwner ? personResolver.resolveOwner(record) :
                personResolver.resolveByCienciaId(people.get(i).cienciaId()));

            contributions.add(contribution);
        }

        return contributions;
    }

    /**
     * Which author is the curriculum owner. The source does not always mark them with
     * {@code self}, so the owner's Ciência ID and then their name forms are tried as well; a name
     * only counts when exactly one author carries it.
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

        var ownerKeys = personResolver.ownerNameKeys(record);
        var match = -1;

        for (int i = 0; i < names.size(); i++) {
            if (ownerKeys.contains(conversionUtil.authorMatchKey(names.get(i)))) {
                if (match >= 0) {
                    match = -2;
                    break;
                }

                match = i;
            }
        }

        if (match < 0 && !people.isEmpty()) {
            migrationLog.valueDropped(HydratorSource.NAME, entityType.name(),
                outputKey(record, output), "owner-match",
                (match == -2 ? "several authors match the owner's name" :
                    "owner not identified") + " among " + people.size() + " authors");
            return -1;
        }

        return match;
    }

    // Only the owner's role is known
    private DocumentContributionType ownerContributionType(HydratorCVModel.CodeValue role) {
        if (Objects.isNull(role) || isBlank(role.code())) {
            return DocumentContributionType.AUTHOR;
        }

        return OWNER_ROLES.getOrDefault(role.code().trim().toUpperCase(Locale.ROOT),
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

        return PUBLICATION_STATUSES.get(status.code().trim().toUpperCase(Locale.ROOT));
    }

    // MAP-000070: one keyword per line, as everywhere else in TeslaRIS
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

        var value = Objects.toString(degreeType.value(), "").toLowerCase(Locale.ROOT);

        if (value.contains("master")) {
            return ThesisType.MASTER;
        }

        if (value.contains("magist")) {
            return ThesisType.MR;
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

    private String languageOf(HydratorCVModel.Curriculum record) {
        return Objects.isNull(record.curriculum()) ? null : record.curriculum().language();
    }

    private boolean isTrue(String value) {
        return "true".equalsIgnoreCase(Objects.toString(value, "").trim());
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }

    private record IdentifierTarget(BiConsumer<DocumentDTO, String> setter, Pattern pattern) {

        IdentifierTarget(BiConsumer<DocumentDTO, String> setter, String regex) {
            // Core matches case-insensitively as well
            this(setter, Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
        }
    }
}
