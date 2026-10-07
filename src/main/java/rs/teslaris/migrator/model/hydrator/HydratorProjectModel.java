package rs.teslaris.migrator.model.hydrator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * SciPROJ project as served by hydrator ({@code /api/projects}); only what the migrator reads.
 */
public class HydratorProjectModel {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProjectDocument(
        String id,
        ProjectRecord record
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProjectRecord(
        Metadata metadata
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(
        Project project
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Project(
        String projectId,
        String acronym,
        List<LangValue> titles,
        List<TypedValue> identifiers,
        String startDate,
        String endDate,
        List<Subject> subjects,
        List<LangValue> keywords,
        List<LangValue> abstracts,
        List<Funded> funded,
        OAMandate oaMandate
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LangValue(
        String language,
        String value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TypedValue(
        String type,
        String value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Subject(
        String scheme,
        String language,
        String value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Funded(
        FundedBy fundedBy,
        FundedAs fundedAs
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FundedBy(
        OrgUnit orgUnit
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrgUnit(
        String id,
        LangValue name,
        String rorId,
        List<TypedValue> identifiers
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FundedAs(
        Funding funding
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Funding(
        String id,
        String grantId,
        TextValue type,
        LangValue name,
        Amount amount,
        List<TypedValue> identifiers,
        Duration duration
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TextValue(
        String value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(
        String currency,
        String value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Duration(
        String startDate,
        String endDate
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OAMandate(
        String mandated,
        String uri
    ) {
    }
}
