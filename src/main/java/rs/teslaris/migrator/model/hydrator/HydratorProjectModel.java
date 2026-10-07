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
        List<LangValue> abstracts
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
}
