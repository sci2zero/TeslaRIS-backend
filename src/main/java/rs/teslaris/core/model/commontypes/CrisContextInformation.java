package rs.teslaris.core.model.commontypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.document.License;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cris_context_information")
public class CrisContextInformation extends BaseEntity {

    @Column(name = "toggle_assessment_module")
    private Boolean toggleAssessmentModule = true;

    @Column(name = "toggle_digital_library")
    private Boolean toggleDigitalLibrary = false;

    @Column(name = "toggle_digital_repository")
    private Boolean toggleDigitalRepository = false;

    @Column(name = "person_national_id_regular_expression")
    private String personNationalIdRegularExpression;

    @Column(name = "project_national_id_regular_expression")
    private String projectNationalIdRegularExpression;

    @Column(name = "organisation_unit_national_id_regular_expression")
    private String organisationUnitNationalIdRegularExpression;

    @Column(name = "document_national_id_regular_expression")
    private String documentNationalIdRegularExpression;

    @Column(name = "metadata_license")
    private License metadataLicense;
}
