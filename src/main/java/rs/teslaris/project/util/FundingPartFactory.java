package rs.teslaris.project.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.funding.FundingPart;
import rs.teslaris.project.repository.funding.FundingApplicationRepository;
import rs.teslaris.project.repository.funding.FundingRepository;
import rs.teslaris.project.repository.project.OrganisationUnitProjectContributionRepository;
import rs.teslaris.project.repository.project.PersonProjectContributionRepository;
import rs.teslaris.project.repository.project.ProjectDocumentRepository;
import rs.teslaris.project.repository.project.ProjectEventRepository;

@Component
@RequiredArgsConstructor
public class FundingPartFactory {

    private final MultilingualContentService multilingualContentService;

    private final CurrencyService currencyService;

    private final FundingRepository fundingRepository;

    private final ProjectEventRepository projectEventRepository;

    private final ProjectDocumentRepository projectDocumentRepository;

    private final FundingApplicationRepository fundingApplicationRepository;

    private final PersonProjectContributionRepository personProjectContributionRepository;

    private final OrganisationUnitProjectContributionRepository
            organisationUnitProjectContributionRepository;

    public FundingPart buildFundingPart(FundingPartDTO dto) {
        var fundingPart = new FundingPart();
        setCommonFields(fundingPart, dto);
        return fundingPart;
    }

    public void setCommonFields(FundingPart fundingPart, FundingPartDTO dto) {
        fundingPart.setDescription(
                multilingualContentService.getMultilingualContent(dto.getDescription()));

        if (Objects.isNull(fundingPart.getAmount())) {
            fundingPart.setAmount(new MonetaryAmount());
        }

        fundingPart.getAmount()
                .setCurrency(currencyService.findOne(dto.getAmount().getCurrencyId()));
        fundingPart.getAmount().setAmount(dto.getAmount().getAmount());

        fundingPart.setFunding(
                resolve(dto.getFundingId(), fundingRepository::findById, "Funding"));

        clearTargets(fundingPart);

        // A part belongs to exactly one target, so the first id present wins - same precedence the
        // standalone endpoint has always used.
        if (Objects.nonNull(dto.getProjectEventId())) {
            fundingPart.setProjectEvent(resolve(dto.getProjectEventId(),
                    projectEventRepository::findById, "ProjectEvent"));
        } else if (Objects.nonNull(dto.getFundingApplicationId())) {
            fundingPart.setFundingApplication(resolve(dto.getFundingApplicationId(),
                    fundingApplicationRepository::findById, "FundingApplication"));
        } else if (Objects.nonNull(dto.getProjectDocumentId())) {
            fundingPart.setProjectDocument(resolve(dto.getProjectDocumentId(),
                    projectDocumentRepository::findById, "ProjectDocument"));
        } else if (Objects.nonNull(dto.getPersonProjectContributionId())) {
            fundingPart.setPersonContribution(resolve(dto.getPersonProjectContributionId(),
                    personProjectContributionRepository::findById, "PersonProjectContribution"));
        } else if (Objects.nonNull(dto.getOrganisationUnitProjectContributionId())) {
            fundingPart.setOrganisationUnitContribution(
                    resolve(dto.getOrganisationUnitProjectContributionId(),
                            organisationUnitProjectContributionRepository::findById,
                            "OrganisationUnitProjectContribution"));
        }
    }

    private void clearTargets(FundingPart fundingPart) {
        fundingPart.setProjectEvent(null);
        fundingPart.setProjectDocument(null);
        fundingPart.setFundingApplication(null);
        fundingPart.setPersonContribution(null);
        fundingPart.setOrganisationUnitContribution(null);
    }

    private <T> T resolve(Integer id, Function<Integer, Optional<T>> lookup, String entityName) {
        if (Objects.isNull(id)) {
            return null;
        }

        return lookup.apply(id).orElseThrow(() -> new NotFoundException(
                "Cannot find entity " + entityName + " with id: " + id));
    }
}
