package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.document.Document;

@Service
public interface DocumentLookupService {

    Document fastDocumentLookup(Integer documentId);

    DocumentPublicationIndex getDocumentIndex(Integer documentId);
}
