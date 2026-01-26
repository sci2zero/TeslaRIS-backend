package rs.teslaris.core.service.interfaces.person;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.PrizeDTO;
import rs.teslaris.core.dto.person.PrizeResponseDTO;
import rs.teslaris.core.indexmodel.PrizeIndex;
import rs.teslaris.core.model.person.Prize;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface PrizeService extends JPAService<Prize> {

    PrizeResponseDTO addPrize(Integer personId, PrizeDTO dto);

    PrizeResponseDTO updatePrize(Integer prizeId, PrizeDTO dto);

    void deletePrize(Integer prizeId, Integer personId);

    DocumentFileResponseDTO addProof(Integer prizeId, DocumentFileDTO proof);

    DocumentFileResponseDTO updateProof(DocumentFileDTO updatedProof);

    void deleteProof(Integer proofId, Integer prizeId);

    CompletableFuture<Void> reindexPrizes();

    void reindexPrizeVolatileInformation(Prize prize, PrizeIndex prizeIndex,
                                         boolean indexRelations,
                                         boolean indexAssessments);

    Page<PrizeIndex> searchPrizes(List<String> tokens, Pageable pageable, Integer personId,
                                  Integer institutionId, Integer commissionId);
}
