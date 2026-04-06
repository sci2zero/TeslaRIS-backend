package rs.teslaris.project.service.interfaces.funding;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.dto.funding.FundingProgramDTO;
import rs.teslaris.project.indexmodel.funding.FundingProgramIndex;
import rs.teslaris.project.model.funding.FundingProgram;

@Service
public interface FundingProgramService extends JPAService<FundingProgram> {

    Page<FundingProgramIndex> searchFundingPrograms(List<String> tokens, LocalDate dateFrom,
                                                    LocalDate dateTo, Integer funderId,
                                                    Pageable pageable);

    FundingProgramDTO readFundingProgram(Integer fundingProgramId);

    FundingProgram createFundingProgram(FundingProgramDTO fundingProgramDTO);

    void updateFundingProgram(Integer fundingProgramId, FundingProgramDTO fundingProgramDTO);

    void deleteFundingProgram(Integer fundingProgramId);

    DocumentFileResponseDTO addFundingProgramDocument(Integer fundingProgramId,
                                                      DocumentFileDTO program);

    DocumentFileResponseDTO updateFundingProgramDocument(DocumentFileDTO updatedProgram);

    void deleteFundingProgramDocument(Integer programFileId, Integer fundingProgramId);

    CompletableFuture<Void> reindexFundingPrograms();

    void indexFundingProgram(FundingProgram fundingProgram, FundingProgramIndex index);
}
