package rs.teslaris.importer.model.converter.load.publication;

import jakarta.annotation.Nullable;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.oaipmh.publication.Publication;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.RecordConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Component
@Slf4j
public class DissertationConverter extends DocumentConverter
    implements RecordConverter<Publication, ThesisDTO> {

    private final OrganisationUnitService organisationUnitService;

    private final LanguageTagService languageTagService;


    public DissertationConverter(
        MultilingualContentConverter multilingualContentConverter,
        PersonContributionConverter personContributionConverter,
        OrganisationUnitService organisationUnitService, LanguageTagService languageTagService) {
        super(multilingualContentConverter, personContributionConverter);
        this.organisationUnitService = organisationUnitService;
        this.languageTagService = languageTagService;
    }

    @Override
    @Nullable
    public ThesisDTO toDTO(Publication record) {
        var dto = new ThesisDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        setCommonFields(record, dto);

        dto.setThesisType(ThesisType.PHD);

        if (Objects.nonNull(record.getLanguage()) && record.getLanguage().equals("sr-Cyrl")) {
            record.setLanguage("SR-CYR");
        }
        dto.setWritingLanguageTagId(
            languageTagService.findLanguageTagByValue(record.getLanguage()).getId());

        if (Objects.isNull(record.getPublishers()) || record.getPublishers().isEmpty()) {
            log.error("Thesis with ID {} has no specified publishers. Skipping.", dto.getOldId());
            return dto;
        }

        var publisher = record.getPublishers().getFirst();
        if (Objects.nonNull(publisher)) {
            if (Objects.nonNull(publisher.getOrgUnit()) &&
                Objects.nonNull(publisher.getOrgUnit().getOldId())) {
                var organisationUnit = organisationUnitService.findOrganisationUnitByOldId(
                    OAIPMHParseUtility.parseBISISID(publisher.getOrgUnit().getOldId()));

                if (Objects.isNull(organisationUnit)) {
                    log.error(
                        "Unable to migrate thesis with ID {}. Because OU with ID {} does not exist.",
                        record.getOldId(), publisher.getOrgUnit().getOldId());
                    return null;
                }

                dto.setOrganisationUnitId(organisationUnit.getId());
            } else {
                dto.setExternalOrganisationUnitName(
                    multilingualContentConverter.toDTO(publisher.getDisplayName()));
            }
        }

        return dto;
    }
}
