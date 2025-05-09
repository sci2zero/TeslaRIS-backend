package rs.teslaris.importer.model.converter.load.publication;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.oaipmh.publication.Publication;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.OAIPMHParseUtility;
import rs.teslaris.importer.utility.RecordConverter;

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
    public ThesisDTO toDTO(Publication record) {
        var dto = new ThesisDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        setCommonFields(record, dto);

        dto.setThesisType(ThesisType.PHD);

        if (record.getLanguage().equals("sr-Cyrl")) {
            record.setLanguage("SR-CYR");
        }
        dto.setWritingLanguageTagId(
            languageTagService.findLanguageTagByValue(record.getLanguage()).getId());

        var affiliation = record.getAuthors().getFirst().getAffiliation();
        if (Objects.nonNull(affiliation)) {
            if (Objects.nonNull(affiliation.getOrgUnit()) &&
                Objects.nonNull(affiliation.getOrgUnit().getId())) {
                dto.setOrganisationUnitId(organisationUnitService.findOrganisationUnitByOldId(
                    OAIPMHParseUtility.parseBISISID(affiliation.getOrgUnit().getOldId())).getId());
            } else {
                dto.setExternalOrganisationUnitName(
                    multilingualContentConverter.toDTO(affiliation.getDisplayName()));
            }
        } else {
            System.out.println("AAAAAAAA");
        }

        return dto;
    }
}
