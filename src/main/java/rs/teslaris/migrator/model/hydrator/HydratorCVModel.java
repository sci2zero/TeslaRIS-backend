package rs.teslaris.migrator.model.hydrator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Migrator-side view of the hydrator curriculum payload.
 * <p>
 * Deliberately partial: only the fields the converters consume are declared, and every record
 * ignores unknown properties, so hydrator can add fields without breaking the migration. Property
 * names follow the hydrator record component names, which is what Jackson serialises (the
 * {@code @Field} annotations there apply to its Mongo mapping, not to JSON).
 */
public class HydratorCVModel {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Curriculum(
        String id,
        String fullName,
        CurriculumData curriculum
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CurriculumData(
        String language,
        String lastModifiedDate,
        IdentifyingInfo identifyingInfo,
        Employments employments,
        Distinctions distinctions,
        Outputs outputs
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IdentifyingInfo(
        PersonInfo personInfo,
        CitationNames citationNames,
        AuthorIdentifiers authorIdentifiers,
        DomainActivities domainActivities,
        Resume resume,
        Emails emails,
        PhoneNumbers phoneNumbers,
        MailingAddresses mailingAddresses,
        WebAddresses webAddresses,
        LanguageCompetencies languageCompetencies
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PersonInfo(
        String fullName,
        String displayName,
        String names,
        String surnames,
        DateOfBirth dateOfBirth,
        Gender gender,
        Photography photography
    ) {
    }

    /**
     * Values are present only when {@code privacyLevel} is {@code publico}; otherwise the source
     * keeps just the privacy level.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DateOfBirth(
        String privacyLevel,
        String year,
        String month,
        String day
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Gender(
        String privacyLevel,
        String code,
        String value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Photography(
        String privacyLevel,
        String fileName
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CitationNames(
        Integer total,
        List<CitationName> citationName
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CitationName(
        String id,
        String privacyLevel,
        String preferredCitationName,
        String value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthorIdentifiers(
        Integer total,
        List<AuthorIdentifier> authorIdentifier
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthorIdentifier(
        String id,
        IdentifierType identifierType,
        String identifier
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IdentifierType(
        String code,
        String value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DomainActivities(
        Integer total,
        List<DomainActivity> domainActivity
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DomainActivity(
        String id,
        ResearchClassification researchClassification,
        String topic,
        Keywords keywords
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResearchClassification(
        String code,
        String value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResearchClassifications(
        Integer total,
        List<ResearchClassification> researchClassification
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Keywords(
        Integer total,
        List<String> keyword
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resume(
        String text
    ) {
    }

    /**
     * Code-and-label pair of a controlled vocabulary value; map on {@code code}, the label's
     * language varies.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CodeValue(
        String code,
        String value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Emails(
        Integer total,
        List<Email> email
    ) {
    }

    /**
     * {@code lastModifiedDate} ({@code -common:last-modified-date}) is not served by hydrator yet
     * for emails, phone numbers and mailing addresses; it stays null until it is.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Email(
        String id,
        String lastModifiedDate,
        String preferredEmail,
        String emailAddress,
        CodeValue emailType
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PhoneNumbers(
        Integer total,
        List<PhoneNumber> phoneNumber
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PhoneNumber(
        String id,
        String lastModifiedDate,
        String preferredPhoneNumber,
        String countryCode,
        String localNumber,
        String extension,
        CodeValue phoneType,
        CodeValue usageType
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MailingAddresses(
        Integer total,
        List<MailingAddress> mailingAddress
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MailingAddress(
        String id,
        String lastModifiedDate,
        String preferredMailingAddress,
        String streetAddress,
        String city,
        String postalCode,
        String provinceState,
        Country country,
        CodeValue addressType
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Country(
        String code,
        String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WebAddresses(
        Integer total,
        List<WebAddress> webAddress
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WebAddress(
        String id,
        String url,
        CodeValue siteType
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LanguageCompetencies(
        Integer total,
        List<LanguageCompetency> languageCompetency
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LanguageCompetency(
        String id,
        CodeValue language,
        String motherTongue,
        CodeValue read,
        CodeValue write,
        CodeValue speak,
        CodeValue understandSpoken,
        CodeValue peerReview
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Distinctions(
        Integer total,
        List<Distinction> distinction
    ) {
    }

    /**
     * {@code effectiveDate} is a bare year string, unlike the date triples used elsewhere.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Distinction(
        String id,
        String privacyLevel,
        DistinctionType distinctionType,
        String name,
        String effectiveDate,
        DateInfo endDate,
        String description,
        Keywords keywords,
        ResearchClassifications researchClassifications
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DistinctionType(
        String code,
        String type
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Employments(
        Integer total,
        List<Employment> employment
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Employment(
        String id,
        List<Institution> institution,
        PositionType positionType,
        PositionTitle positionTitle,
        DateInfo startDate,
        DateInfo endDate
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PositionType(
        String code,
        String value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PositionTitle(
        String code,
        String title
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Institution(
        String name,
        String url
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DateInfo(
        String year,
        String month,
        String day
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Outputs(
        Integer total,
        List<Output> output
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Output(
        String id,
        OutputType outputType,
        JournalArticle journalArticle,
        Dissertation dissertation
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutputType(
        String code,
        String type
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JournalArticle(
        String articleTitle,
        String journal,
        String volume,
        String issue,
        String pageFrom,
        String pageTo,
        DateInfo publicationDate,
        String url,
        OutputIdentifiers identifiers,
        OutputAuthors authors,
        CodeValue authoringRole,
        CodeValue publicationStatus,
        PublicationLocation publicationLocation,
        String refereed,
        String openAccess,
        ResearchClassifications researchClassifications,
        Keywords keywords
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PublicationLocation(
        String city,
        Country country
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResearchClassifications(
        Integer total,
        List<ResearchClassification> researchClassification
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dissertation(
        String title,
        DegreeType degreeType,
        DateInfo completionDate,
        String url,
        OutputIdentifiers identifiers,
        OutputAuthors authors
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DegreeType(
        String code,
        String value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutputIdentifiers(
        Integer total,
        List<OutputIdentifier> identifier
    ) {
    }

    /**
     * {@code relationshipType.code} is {@code P} (Self) for the work's own id and {@code PD}
     * (Part of) for its container's - the ISSN of a journal, the ISBN of a book.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutputIdentifier(
        IdentifierType identifierType,
        String identifier,
        CodeValue relationshipType
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutputAuthors(
        Integer total,
        List<OutputAuthor> author,
        String citation
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    /**
     * The source keeps the whole name in one string ({@code "Surname, Given"} or free form);
     * {@code self} marks the curriculum owner.
     */
    public record OutputAuthor(
        String cienciaId,
        Boolean self,
        String name
    ) {
    }
}
