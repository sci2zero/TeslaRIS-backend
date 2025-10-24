package rs.teslaris.reporting.service.interfaces.leaderboards;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.util.functional.Pair;

@Service
public interface GlobalLeaderboardService {

    List<Pair<PersonIndex, Long>> getPersonsWithMostCitations();

    List<Pair<OrganisationUnitIndex, Long>> getInstitutionsWithMostCitations();

    List<Pair<DocumentPublicationIndex, Long>> getDocumentsWithMostCitations();
}
