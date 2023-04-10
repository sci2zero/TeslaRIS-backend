package rs.teslaris.core.model.institution;

import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentFile;

import java.time.LocalDate;
import java.util.Set;

public class OrganisationUnitsRelation {
    Set<MultiLingualContent> sourceAffiliationStatement;
    Set<MultiLingualContent> targetAffiliationStatement;
    OrganisationUnitRelationType relationType;
    LocalDate from;
    LocalDate to;
    ApproveStatus approveStatus;
    Set<DocumentFile> proofs;
    OrganisationUnit sourceOrganisationUnit;
    OrganisationUnit targetOrganisationUnit;
}
