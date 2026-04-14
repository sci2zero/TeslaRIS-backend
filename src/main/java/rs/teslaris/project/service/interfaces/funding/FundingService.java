package rs.teslaris.project.service.interfaces.funding;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.dto.funding.FundingDTO;
import rs.teslaris.project.indexmodel.funding.FundingIndex;
import rs.teslaris.project.model.funding.Funding;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public interface FundingService extends JPAService<Funding> {

    Page<FundingIndex> searchFunding(List<String> tokens, LocalDate dateFrom,
                                     LocalDate dateTo, Integer projectId,
                                     Integer fundingCallId, Integer funderId,
                                     Pageable pageable);

    FundingDTO readFunding(Integer fundingId);

    Funding createFunding(FundingDTO fundingDTO);

    void updateFunding(Integer fundingId, FundingDTO fundingDTO);

    void deleteFunding(Integer fundingId);

    CompletableFuture<Void> reindexFunding();

    void indexFunding(Funding funding, FundingIndex index);

}
