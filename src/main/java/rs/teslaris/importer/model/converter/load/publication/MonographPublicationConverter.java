package rs.teslaris.importer.model.converter.load.publication;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublicationType;
import rs.teslaris.core.model.document.MonographType;
import rs.teslaris.core.model.oaipmh.publication.Publication;
import rs.teslaris.core.repository.document.MonographRepository;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.RecordConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Component
@Slf4j
public class MonographPublicationConverter extends DocumentConverter implements
    RecordConverter<Publication, MonographPublicationDTO> {

    private final DocumentPublicationService documentPublicationService;

    private final MonographRepository monographRepository;


    @Autowired
    public MonographPublicationConverter(MultilingualContentConverter multilingualContentConverter,
                                         PublisherConverter publisherConverter,
                                         BookSeriesService bookSeriesService,
                                         JournalService journalService,
                                         PersonContributionConverter personContributionConverter,
                                         DocumentPublicationService documentPublicationService,
                                         MonographRepository monographRepository) {
        super(multilingualContentConverter, publisherConverter, bookSeriesService, journalService,
            personContributionConverter);
        this.documentPublicationService = documentPublicationService;
        this.monographRepository = monographRepository;
    }

    @Override
    public MonographPublicationDTO toDTO(Publication record) {
        var dto = new MonographPublicationDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        if (record.getType().endsWith("c_3248")) {
            dto.setMonographPublicationType(MonographPublicationType.CHAPTER);
        } else {
            dto.setMonographPublicationType(MonographPublicationType.RESEARCH_ARTICLE);
        }

        setCommonFields(record, dto);

        dto.setArticleNumber(record.getNumber());
        dto.setStartPage(record.getStartPage());

        if (Objects.nonNull(dto.getStartPage()) && dto.getStartPage().contains("\\")) {
            dto.setStartPage(dto.getStartPage().replace("\\", ""));
        }

        dto.setEndPage(record.getEndPage());

        var monograph = documentPublicationService.findDocumentByOldId(
            OAIPMHParseUtility.parseBISISID(record.getPartOf().getPublication().getOldId()));
        if (Objects.isNull(monograph)) {
            log.warn("No saved monograph with id: {}",
                record.getPartOf().getPublication().getOldId());
            return null;
        }

        dto.setMonographId(monograph.getId());

        if (((Monograph) monograph).getMonographType().equals(MonographType.BOOK)) {
            ((Monograph) monograph).setMonographType(MonographType.RESEARCH_MONOGRAPH);
            monographRepository.save((Monograph) monograph);
        }

        return dto;
    }
}
