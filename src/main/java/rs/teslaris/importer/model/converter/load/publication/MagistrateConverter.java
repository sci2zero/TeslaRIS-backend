package rs.teslaris.importer.model.converter.load.publication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.oaipmh.publication.Publication;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.RecordConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Component
@Slf4j
public class MagistrateConverter extends DocumentConverter
    implements RecordConverter<Publication, ThesisDTO> {

    private final OrganisationUnitService organisationUnitService;

    private final LanguageTagService languageTagService;


    @Autowired
    public MagistrateConverter(MultilingualContentConverter multilingualContentConverter,
                               PublisherConverter publisherConverter,
                               BookSeriesService bookSeriesService,
                               PersonContributionConverter personContributionConverter,
                               OrganisationUnitService organisationUnitService,
                               LanguageTagService languageTagService) {
        super(multilingualContentConverter, publisherConverter, bookSeriesService,
            personContributionConverter);
        this.organisationUnitService = organisationUnitService;
        this.languageTagService = languageTagService;
    }

    @Override
    public ThesisDTO toDTO(Publication record) {
        var dto = new ThesisDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        setCommonFields(record, dto);

        dto.setThesisType(ThesisType.MR);

        try {
            setCommonThesisFields(record, dto, languageTagService, organisationUnitService);
        } catch (NotFoundException ignored) {
            return null;
        }

        return dto;
    }
}
