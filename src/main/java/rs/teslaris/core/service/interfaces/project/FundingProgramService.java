package rs.teslaris.core.service.interfaces.project;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.project.FundingProgramDTO;
import rs.teslaris.core.indexmodel.project.FundingProgramIndex;
import rs.teslaris.core.model.project.FundingProgram;
import rs.teslaris.core.service.interfaces.JPAService;

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

    void reindexFundingPrograms();

    void indexFundingProgram(FundingProgram fundingProgram, FundingProgramIndex index);
}
