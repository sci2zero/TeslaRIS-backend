package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.ExhibitionDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.model.document.Exhibition;

@Service
public interface ExhibitionService {

    Page<ExhibitionDTO> readAllExhibitions(Pageable pageable);

    Page<EventIndex> searchExhibitions(List<String> tokens, Pageable pageable,
                                       Boolean returnOnlyNonSerialEvents,
                                       Boolean returnOnlySerialEvents,
                                       Integer commissionInstitutionId,
                                       Integer commissionId, Boolean emptyEventsOnly,
                                       Boolean noContributionEventsOnly);

    Page<EventIndex> searchExhibitionsForImport(List<String> names, String dateFrom, String dateTo);

    ExhibitionDTO readExhibition(Integer exhibitionId);

    Exhibition findExhibitionById(Integer exhibitionId);

    Exhibition findRaw(Integer exhibitionId);

    Exhibition createExhibition(ExhibitionDTO exhibitionDTO, Boolean index);

    void updateExhibition(Integer exhibitionId, ExhibitionDTO exhibitionDTO);

    void deleteExhibition(Integer exhibitionId);

    void forceDeleteExhibition(Integer exhibitionId);

    CompletableFuture<Void> reindexExhibitions();

    void reindexExhibition(Integer exhibitionId);

    void reindexVolatileExhibitionInformation(Integer exhibitionId);

    void reorderExhibitionContributions(Integer exhibitionId, Integer contributionId,
                                        Integer oldContributionOrderNumber,
                                        Integer newContributionOrderNumber);

    ExhibitionDTO readExhibitionByOldId(Integer oldId);

    boolean isIdentifierInUse(String identifier, Integer exhibitionId);

    void indexExhibition(Exhibition exhibition);

    void save(Exhibition exhibition);
}
