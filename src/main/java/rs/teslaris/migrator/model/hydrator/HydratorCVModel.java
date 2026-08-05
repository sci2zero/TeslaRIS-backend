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
        IdentifyingInfo identifyingInfo,
        Employments employments,
        Outputs outputs
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IdentifyingInfo(
        PersonInfo personInfo,
        AuthorIdentifiers authorIdentifiers,
        Resume resume
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PersonInfo(
        String fullName,
        String names,
        String surnames
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
    public record Resume(
        String text
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
        OutputAuthors authors
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutputIdentifier(
        IdentifierType identifierType,
        String identifier
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutputAuthors(
        Integer total,
        List<OutputAuthor> author
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutputAuthor(
        String cienciaId,
        String name,
        String surname
    ) {
    }
}
