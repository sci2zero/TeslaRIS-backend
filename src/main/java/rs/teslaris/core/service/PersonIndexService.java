package rs.teslaris.core.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.PersonIndex;

@Service
public interface PersonIndexService {

    Page<PersonIndex> findPeopleByNameAndEmployment(List<String> tokens, Pageable pageable);

    Page<PersonIndex> findPeopleForOrganisationUnit(Integer employmentInstitutionId,
                                                    Pageable pageable);
}
