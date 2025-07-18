package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.document.MonographPublication;

@Service
public interface MonographPublicationService {

    MonographPublication findMonographPublicationById(Integer monographPublicationId);

    MonographPublicationDTO readMonographPublicationById(Integer monographPublicationId);

    MonographPublication createMonographPublication(MonographPublicationDTO monographPublicationDTO,
                                                    Boolean index);

    List<DocumentPublicationIndex> findAuthorsPublicationsForMonograph(Integer monographId,
                                                                       Integer authorId);

    Page<DocumentPublicationIndex> findAllPublicationsForMonograph(Integer monographId,
                                                                   Pageable pageable);

    void editMonographPublication(Integer monographId,
                                  MonographPublicationDTO monographPublicationDTO);

    void deleteMonographPublication(Integer monographPublicationId);

    void reindexMonographPublications();

    void indexMonographPublication(MonographPublication monographPublication,
                                   DocumentPublicationIndex index);

    void indexMonographPublication(MonographPublication monographPublication);
}
