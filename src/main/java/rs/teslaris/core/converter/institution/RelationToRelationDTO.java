package rs.teslaris.core.converter.institution;

import java.util.stream.Collectors;
import rs.teslaris.core.converter.commontypes.MultilingualContentToMultilingualContentDTO;
import rs.teslaris.core.converter.document.DocumentFileToDocumentFileResponseDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationResponseDTO;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;

@Deprecated
public class RelationToRelationDTO {

    public static OrganisationUnitsRelationResponseDTO toResponseDTO(
        OrganisationUnitsRelation relation) {
        var relationResponse = new OrganisationUnitsRelationResponseDTO();
        relationResponse.setId(relation.getId());
        relationResponse.setSourceAffiliationStatement(
            MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
                relation.getSourceAffiliationStatement()));
        relationResponse.setTargetAffiliationStatement(
            MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
                relation.getTargetAffiliationStatement()));

        relationResponse.setDateFrom(relation.getDateFrom());
        relationResponse.setDateTo(relation.getDateTo());
        relationResponse.setRelationType(relation.getRelationType());

        relationResponse.setProofs(relation.getProofs().stream()
            .map(DocumentFileToDocumentFileResponseDTO::toDTO).collect(
                Collectors.toList()));

        relationResponse.setSourceOrganisationUnitName(
            MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
                relation.getSourceOrganisationUnit().getName()));
        relationResponse.setTargetOrganisationUnitName(
            MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
                relation.getTargetOrganisationUnit().getName()));

        return relationResponse;
    }
}
