package rs.teslaris.core.service.interfaces.project;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.project.FundingCallDTO;
import rs.teslaris.core.indexmodel.project.FundingCallIndex;
import rs.teslaris.core.model.project.FundingCall;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface FundingCallService extends JPAService<FundingCall> {

    Page<FundingCallIndex> searchFundingCalls(List<String> tokens, LocalDate dateFrom,
                                              LocalDate dateTo, Integer fundingProgramId,
                                              Pageable pageable);

    FundingCallDTO readFundingCall(Integer fundingCallId);

    FundingCall createFundingCall(FundingCallDTO fundingCallDTO);

    void updateFundingCall(Integer fundingCallId, FundingCallDTO fundingCallDTO);

    void deleteFundingCall(Integer fundingCallId);

    DocumentFileResponseDTO addFundingCallDocument(Integer fundingCallId,
                                                   DocumentFileDTO program);

    DocumentFileResponseDTO updateFundingCallDocument(DocumentFileDTO updatedProgram);

    void deleteFundingCallDocument(Integer programFileId, Integer fundingCallId);

    void reindexFundingCalls();

    void indexFundingCall(FundingCall fundingCall, FundingCallIndex index);
}
