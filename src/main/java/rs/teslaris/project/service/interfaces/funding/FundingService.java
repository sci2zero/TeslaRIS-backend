package rs.teslaris.project.service.interfaces.funding;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.dto.funding.FundingDTO;
import rs.teslaris.project.indexmodel.funding.FundingCallIndex;
import rs.teslaris.project.indexmodel.funding.FundingIndex;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.model.funding.FundingCall;

import java.util.concurrent.CompletableFuture;

@Service
public interface FundingService extends JPAService<Funding> {

    FundingDTO readFunding(Integer fundingId);

    Funding createFunding(FundingDTO fundingDTO);

    void updateFunding(Integer fundingId, FundingDTO fundingDTO);

    void deleteFunding(Integer fundingId);

    CompletableFuture<Void> reindexFunding();

    void indexFunding(Funding funding, FundingIndex index);

}
