package rs.teslaris.project.service.interfaces.funding;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.dto.funding.FundingApplicationDTO;
import rs.teslaris.project.indexmodel.funding.FundingApplicationIndex;
import rs.teslaris.project.model.funding.FundingApplication;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;

import java.util.concurrent.CompletableFuture;

@Service
public interface FundingApplicationService extends JPAService<FundingApplication> {

    FundingApplicationDTO readFundingApplication(Integer fundingApplicationId);

    FundingApplication createFundingApplication(FundingApplicationDTO fundingApplicationDTO);

    void updateFundingApplication(Integer fundingApplicationId, FundingApplicationDTO fundingApplicationDTO);

    void deleteFundingApplication(Integer fundingApplicationId);

    CompletableFuture<Void> reindexFundingApplications();

    void indexFundingApplication(FundingApplication fundingApplication, FundingApplicationIndex index);

    DocumentFileResponseDTO addFundingApplicationDocument(Integer fundingApplicationId,
                                                          DocumentFileDTO documentFile);

    DocumentFileResponseDTO updateFundingApplicationDocument(DocumentFileDTO documentFile);

    void deleteFundingApplicationDocument(Integer documentFileId, Integer fundingApplicationId);
}
