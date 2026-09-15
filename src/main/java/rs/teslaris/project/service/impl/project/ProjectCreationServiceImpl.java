package rs.teslaris.project.service.impl.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.service.interfaces.funding.FundingService;
import rs.teslaris.project.service.interfaces.project.ProjectCreationService;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@Service
@RequiredArgsConstructor
public class ProjectCreationServiceImpl implements ProjectCreationService {

    private final ProjectService projectService;

    private final FundingService fundingService;

    @Override
    @Transactional
    public Project createProject(ProjectDTO projectDTO) {
        var savedProject = projectService.createProject(projectDTO);

        var funding = projectDTO.getFunding();

        if (Objects.nonNull(funding)) {
            funding.setProjectId(savedProject.getId());
            funding.setFundingParts(buildFundingParts(savedProject, projectDTO));

            fundingService.createFunding(funding);
        }

        return savedProject;
    }

    private List<FundingPartDTO> buildFundingParts(Project project, ProjectDTO projectDTO) {
        var amountsByOrderNumber = new HashMap<Integer, MonetaryAmountDTO>();
        projectDTO.getOrganisations().forEach(organisation -> {
            if (Objects.nonNull(organisation.getNetContribution())) {
                amountsByOrderNumber.put(organisation.getOrderNumber(),
                        organisation.getNetContribution());
            }
        });

        var fundingParts = new ArrayList<FundingPartDTO>();

        project.getOrganisations().forEach(contribution -> {
            var amount = amountsByOrderNumber.get(contribution.getOrderNumber());

            if (Objects.isNull(amount) || amount.getAmount() <= 0) {
                return;
            }

            var fundingPart = new FundingPartDTO();
            fundingPart.setAmount(amount);
            fundingPart.setDescription(describe(contribution));
            fundingPart.setOrganisationUnitProjectContributionId(contribution.getId());

            fundingParts.add(fundingPart);
        });

        return fundingParts;
    }

    private List<MultilingualContentDTO> describe(
            OrganisationUnitProjectContribution contribution) {
        return MultilingualContentConverter.getMultilingualContentDTO(
                Objects.nonNull(contribution.getOrganisationUnit())
                        ? contribution.getOrganisationUnit().getName()
                        : contribution.getDisplayOrganisationUnit());
    }
}
