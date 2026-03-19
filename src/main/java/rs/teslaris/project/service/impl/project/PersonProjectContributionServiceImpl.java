package rs.teslaris.project.service.impl.project;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.project.model.project.PersonProjectContribution;
import rs.teslaris.project.repository.project.PersonProjectContributionRepository;
import rs.teslaris.project.service.interfaces.project.PersonProjectContributionService;

@Service
@RequiredArgsConstructor
public class PersonProjectContributionServiceImpl extends JPAServiceImpl<PersonProjectContribution>
    implements
    PersonProjectContributionService {

    private final PersonProjectContributionRepository personProjectContributionRepository;


    @Override
    protected JpaRepository<PersonProjectContribution, Integer> getEntityRepository() {
        return personProjectContributionRepository;
    }
}
