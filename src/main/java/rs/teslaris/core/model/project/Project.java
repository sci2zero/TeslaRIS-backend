package rs.teslaris.core.model.project;

import rs.teslaris.core.model.commontypes.MultiLingualContent;

import java.time.LocalDate;
import java.util.Set;

public class Project {
    Set<MultiLingualContent> name;
    Set<MultiLingualContent> description;
    Set<MultiLingualContent> nameAbbreviation;
    Set<MultiLingualContent> keywords;
    Set<MultiLingualContent> note;
    Set<String> uris;
    Set<PersonProjectContribution> contributors;
    Set<ProjectDocument> documents;
    LocalDate from;
    LocalDate to;
    Set<ProjectStatus> statuses;
    ProjectType type;
    Set<Funding> fundings;
}
