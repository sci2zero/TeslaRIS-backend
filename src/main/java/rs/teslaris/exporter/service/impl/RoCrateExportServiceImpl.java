package rs.teslaris.exporter.service.impl;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.PublisherPublishable;
import rs.teslaris.core.model.rocrate.ContextualEntity;
import rs.teslaris.core.model.rocrate.PublicationBase;
import rs.teslaris.core.model.rocrate.RoCrate;
import rs.teslaris.core.model.rocrate.RoCrateDataset;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.service.interfaces.RoCrateExportService;

@Service
@RequiredArgsConstructor
public class RoCrateExportServiceImpl implements RoCrateExportService {

    private final DocumentPublicationService documentPublicationService;

    @Value("${export.internal-identifier.prefix}")
    private String identifierPrefix;


    @Override
    public RoCrate createMetadataInfo(Integer documentId) {
        var metadataInfo = new RoCrate();

        var document = fetchDocumentForPacking(documentId);
        if (Objects.isNull(document)) {
            return null;
        }
        var documentIdentifier = constructDocumentIdentifier(document);

        if (document instanceof Dataset) {
            var metadata = new RoCrateDataset();
            setCommonFields(metadata, document, documentIdentifier);
            setPublisherInfo(metadata, (PublisherPublishable) document);
        } else if (document instanceof JournalPublication) {
            var metadata = new rs.teslaris.core.model.rocrate.JournalPublication();
            metadata.setIssn(
                StringUtil.valueExists(
                    ((JournalPublication) document).getJournal().getEISSN()) ?
                    ((JournalPublication) document).getJournal().getEISSN() :
                    ((JournalPublication) document).getJournal().getPrintISSN()
            );
        } else if (document instanceof ProceedingsPublication) {
            var metadata = new rs.teslaris.core.model.rocrate.ProceedingsPublication();
            metadata.setIsbn(
                StringUtil.valueExists(
                    ((ProceedingsPublication) document).getProceedings().getEISBN()) ?
                    ((ProceedingsPublication) document).getProceedings().getEISBN() :
                    ((ProceedingsPublication) document).getProceedings().getPrintISBN()
            );
        }

        return metadataInfo;
    }

    public Document fetchDocumentForPacking(Integer documentId) {
        return documentPublicationService.findDocumentById(documentId);
    }

    public String constructDocumentIdentifier(Document document) {
        if (StringUtil.valueExists(document.getDoi())) {
            return "https://doi.org/" + document.getDoi();
        } else if (StringUtil.valueExists(document.getScopusId())) {
            return "https://www.scopus.com/pages/publications/" + document.getScopusId();
        } else if (StringUtil.valueExists(document.getOpenAlexId())) {
            return "https://openalex.org/" + document.getOpenAlexId();
        }
        // TODO: WOS???

        return identifierPrefix + document.getId();
    }

    private void setCommonFields(PublicationBase metadata, Document document,
                                 String primaryIdentifier) {
        metadata.setId(primaryIdentifier);
        metadata.setTitle(StringUtil.getStringContent(document.getTitle(),
            LanguageAbbreviations.ENGLISH));
        metadata.setAbstractText(StringUtil.getStringContent(document.getDescription(),
            LanguageAbbreviations.ENGLISH));
        metadata.setPublicationYear(document.getDocumentDate());

        metadata.setDoi(document.getDoi());

        if (primaryIdentifier.startsWith("http")) {
            metadata.setCiteAs(primaryIdentifier);
        }
    }

    private void setPublisherInfo(PublicationBase metadata, PublisherPublishable document) {
        if (Objects.nonNull(document.getPublisher())) {
            metadata.setPublisher(
                new ContextualEntity(identifierPrefix + document.getPublisher().getId(),
                    "Publisher"));
        }
    }
}
