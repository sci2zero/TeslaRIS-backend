package rs.teslaris.core.converter.institution;

import java.util.stream.Collectors;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.document.DocumentFileToDocumentFileResponseDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationResponseDTO;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;

public class RelationConverter {

    public static OrganisationUnitsRelationResponseDTO toResponseDTO(
        OrganisationUnitsRelation relation) {
        var relationResponse = new OrganisationUnitsRelationResponseDTO();
        relationResponse.setId(relation.getId());
        relationResponse.setSourceAffiliationStatement(
            MultilingualContentConverter.getMultilingualContentDTO(
                relation.getSourceAffiliationStatement()));
        relationResponse.setTargetAffiliationStatement(
            MultilingualContentConverter.getMultilingualContentDTO(
                relation.getTargetAffiliationStatement()));

        relationResponse.setDateFrom(relation.getDateFrom());
        relationResponse.setDateTo(relation.getDateTo());
        relationResponse.setRelationType(relation.getRelationType());

        relationResponse.setProofs(relation.getProofs().stream()
            .map(DocumentFileToDocumentFileResponseDTO::toDTO).collect(
                Collectors.toList()));

        relationResponse.setSourceOrganisationUnitName(
            MultilingualContentConverter.getMultilingualContentDTO(
                relation.getSourceOrganisationUnit().getName()));
        relationResponse.setTargetOrganisationUnitName(
            MultilingualContentConverter.getMultilingualContentDTO(
                relation.getTargetOrganisationUnit().getName()));

        return relationResponse;
    }
}
