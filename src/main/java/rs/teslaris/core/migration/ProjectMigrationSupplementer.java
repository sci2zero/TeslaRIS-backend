package rs.teslaris.core.migration;

import java.util.List;
import rs.teslaris.core.dto.project.PersonProjectContributionDTO;
import rs.teslaris.core.dto.project.ProjectDTO;

/**
 * Supplements a project that an external source matched, so that an importing module does not
 * have to depend on the project module. Implemented there.
 */
public interface ProjectMigrationSupplementer {

    /**
     * Fills only the empty fields of the project and adds the team members it does not have yet;
     * nothing the project already has is overwritten. A member already in the team gets a
     * missing role and dates. The consortium and the fundings are left as they are.
     *
     * @param projectId the matched project
     * @param source    the project as the source describes it
     * @param team      team members as the source describes them, linked persons resolved
     * @return a reason per source value that was not taken over because the project already had a
     * different one, for the caller to report
     */
    List<String> supplement(Integer projectId, ProjectDTO source,
                            List<PersonProjectContributionDTO> team);
}
