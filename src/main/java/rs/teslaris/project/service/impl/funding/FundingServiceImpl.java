package rs.teslaris.project.service.impl.funding;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.DateRangeException;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.project.converter.funding.FundingConverter;
import rs.teslaris.project.dto.funding.FundingDTO;
import rs.teslaris.project.indexmodel.funding.FundingIndex;
import rs.teslaris.project.indexrepository.funding.FundingIndexRepository;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.repository.funding.FundingRepository;
import rs.teslaris.project.service.interfaces.funding.FundingCallService;
import rs.teslaris.project.service.interfaces.funding.FundingService;
import rs.teslaris.project.service.interfaces.project.ProjectService;

import java.util.HashSet;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FundingServiceImpl extends JPAServiceImpl<Funding> implements FundingService {

    private final FundingRepository fundingRepository;

    private final ProjectService projectService;

    private final FundingCallService fundingCallService;

    private final OrganisationUnitService organisationUnitService;

    private final MultilingualContentService multilingualContentService;

    private final ResearchAreaService researchAreaService;

    private final CurrencyService currencyService;

    private final FundingIndexRepository fundingIndexRepository;

    @Override
    protected JpaRepository<Funding, Integer> getEntityRepository() {
        return fundingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public FundingDTO readFunding(Integer fundingId) {
        return FundingConverter.toDTO(findOne(fundingId));
    }

    @Override
    @Transactional
    public Funding createFunding(FundingDTO fundingDTO) {
        var newFunding = new Funding();

        setCommonFields(newFunding, fundingDTO);

        var savedFundingCall = save(newFunding);

        fundingIndexRepository.save(
                indexCommonFields(savedFundingCall, new FundingIndex()));

        return savedFundingCall;
    }

    @Override
    @Transactional
    public void updateFunding(Integer fundingId,
                                  FundingDTO fundingDTO) {
        var fundingToUpdate = findOne(fundingId);

        clearCommonFields(fundingToUpdate);
        setCommonFields(fundingToUpdate, fundingDTO);

        fundingIndexRepository.findFundingIndexByDatabaseId(fundingId)
                .ifPresent(index -> {
                    indexCommonFields(fundingToUpdate, index);
                    fundingIndexRepository.save(index);
                });
    }

    private void setCommonFields(Funding funding, FundingDTO fundingDTO) {
        if (Objects.nonNull(fundingDTO.getDateFrom()) &&
                Objects.nonNull(fundingDTO.getDateTo()) &&
                fundingDTO.getDateTo().isBefore(fundingDTO.getDateFrom())) {
            throw new DateRangeException("Funding must start before it ends.");
        }

        if (Objects.nonNull(fundingDTO.getProjectId())) {
            var project = projectService.findOne(fundingDTO.getProjectId());
            funding.setProject(project);
        } else {
            throw new ReferenceConstraintException("Funding must be bound to a project.");
        }

        if (Objects.nonNull(fundingDTO.getFundingCallId())) {
            var fundingCall = fundingCallService.findOne(fundingDTO.getFundingCallId());
            funding.setFundingCall(fundingCall);
        } else {
            funding.setFundingCall(null);
        }

        if (Objects.nonNull(fundingDTO.getFunderId())) {
            funding.setFunder(organisationUnitService.findOne(fundingDTO.getFunderId()));
        } else {
            funding.setFunder(null);
        }

        funding.setDateSubmitted(fundingDTO.getDateSubmitted());
        funding.setDateAwarded(fundingDTO.getDateAwarded());
        funding.setDateFrom(fundingDTO.getDateFrom());
        funding.setDateTo(fundingDTO.getDateTo());

        funding.setName(multilingualContentService.getMultilingualContent(fundingDTO.getName()));
        funding.setDescription(multilingualContentService.getMultilingualContent(fundingDTO.getDescription()));
        funding.setNameAbbreviation(multilingualContentService.getMultilingualContent(fundingDTO.getNameAbbreviation()));
        funding.setKeywords(multilingualContentService.getMultilingualContent(fundingDTO.getKeywords()));

        funding.setDisplayCall(multilingualContentService.getMultilingualContent(fundingDTO.getDisplayCall()));
        funding.setDisplayProgram(multilingualContentService.getMultilingualContent(fundingDTO.getDisplayProgram()));
        funding.setDisplayFunder(multilingualContentService.getMultilingualContent(fundingDTO.getDisplayFunder()));

        var researchAreas = researchAreaService.getResearchAreasByIds(fundingDTO.getResearchAreasId().stream().toList());
        funding.setResearchAreas(new HashSet<>(researchAreas));

        funding.setFundingTypes(fundingDTO.getFundingTypes());

        if (Objects.nonNull(fundingDTO.getAmount())) {
            if (Objects.isNull(funding.getAmount())) {
                funding.setAmount(new MonetaryAmount());
            }
            funding.getAmount().setCurrency(currencyService.findOne(fundingDTO.getAmount().getCurrencyId()));
            funding.getAmount().setAmount(fundingDTO.getAmount().getAmount());
        } else {
            funding.setAmount(null);
        }

        funding.setUris(fundingDTO.getUris());
        funding.setDoi(fundingDTO.getDoi());
        funding.setGrantAgreementId(fundingDTO.getGrantAgreementId());
        funding.setCompetitive(fundingDTO.getCompetitive());
        funding.setRenewable(fundingDTO.getRenewable());
        funding.setOaMandated(fundingDTO.getOaMandated());
        funding.setOaMandateUrl(fundingDTO.getOaMandateUrl());
        funding.setInternalIdentifiers(fundingDTO.getInternalIdentifiers());
    }

    private void clearCommonFields(Funding funding) {
        funding.getName().clear();
        funding.getDescription().clear();
        funding.getNameAbbreviation().clear();
        funding.getKeywords().clear();
        funding.getDisplayCall().clear();
        funding.getDisplayProgram().clear();
        funding.getDisplayFunder().clear();
        funding.getResearchAreas().clear();
    }

    private FundingIndex indexCommonFields(Funding funding,
                                           FundingIndex index) {
        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
                funding.getName(), true);

        if (srContent.isEmpty() && !otherContent.isEmpty()) {
            srContent.append(otherContent);
        } else if (!srContent.isEmpty() && otherContent.isEmpty()) {
            otherContent.append(srContent);
        }

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
                funding.getNameAbbreviation(), false);

        StringUtil.removeTrailingDelimiters(srContent, otherContent);
        index.setNameSr(!srContent.isEmpty() ? srContent.toString() : otherContent.toString());
        index.setNameSrSortable(index.getNameSr());
        index.setNameOther(
                !otherContent.isEmpty() ? otherContent.toString() : srContent.toString());
        index.setNameOtherSortable(index.getNameOther());

        if (Objects.nonNull(funding.getProject())) {
            index.setProjectId(funding.getProject().getId());
        }

        if (Objects.nonNull(funding.getFundingCall())) {
            index.setFundingCallId(funding.getFundingCall().getId());
        }

        if (Objects.nonNull(funding.getFunder())) {
            index.setFunderId(funding.getFunder().getId());
        }

        index.setDatabaseId(funding.getId());
        index.setDateFrom(funding.getDateFrom());
        index.setDateTo(funding.getDateTo());

        return index;
    }

}
