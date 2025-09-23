package rs.teslaris.core.service.interfaces.document;

import rs.teslaris.core.dto.document.CitationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;

public interface CitationService {

    CitationResponseDTO craftCitations(DocumentPublicationIndex index, String languageCode);

    String craftCitationInGivenStyle(String style, DocumentPublicationIndex index,
                                     String languageCode);

    CitationResponseDTO craftCitations(Integer documentId, String languageCode);
}
