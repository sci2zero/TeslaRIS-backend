package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.model.document.MonographPublication;

@Service
public interface MonographPublicationService {

    MonographPublicationDTO readMonographPublicationById(Integer monographPublicationId);

    MonographPublication createMonographPublication(MonographPublicationDTO monographPublicationDTO,
                                                    Boolean index);

    void updateMonographPublication(Integer monographId,
                                    MonographPublicationDTO monographPublicationDTO);

    void deleteMonographPublication(Integer monographPublicationId);

    void reindexMonographPublications();
}
