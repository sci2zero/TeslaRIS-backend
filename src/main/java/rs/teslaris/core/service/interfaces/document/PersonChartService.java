package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import rs.teslaris.core.service.impl.document.PersonChartServiceImpl;

public interface PersonChartService {

    List<PersonChartServiceImpl.YearlyCounts> getPublicationCountsForPerson(Integer personId,
                                                                            Integer startYear,
                                                                            Integer endYear);
}
