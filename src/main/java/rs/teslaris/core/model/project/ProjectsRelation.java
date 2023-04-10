package rs.teslaris.core.model.project;

import java.time.LocalDate;

public class ProjectsRelation {
    ProjectsRelationType relationType;
    LocalDate from;
    LocalDate to;
    Project sourceProject;
    Project targetProject;
}
