package rs.teslaris.core.model.person;

import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.institution.OrganisationUnit;

import java.time.LocalDate;
import java.util.Set;

public class Involvement {
    LocalDate from;
    LocalDate to;
    ApproveStatus approveStatus;
    Set<DocumentFile> proofs;
    InvolvementType involvementType;
    Set<MultiLingualContent> affiliationStatement;
    Person personInvolved;
    OrganisationUnit organisationUnit;
}
