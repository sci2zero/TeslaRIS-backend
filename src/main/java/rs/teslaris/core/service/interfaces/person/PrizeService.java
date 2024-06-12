package rs.teslaris.core.service.interfaces.person;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.PrizeDTO;
import rs.teslaris.core.dto.person.PrizeResponseDTO;
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
}
