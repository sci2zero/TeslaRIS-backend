package rs.teslaris.core.model.commontypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
    private Boolean toggleDigitalLibrary = true;

    @Column(name = "toggle_digital_repository")
    private Boolean toggleDigitalRepository = true;

    @Column(name = "toggle_registration")
    private Boolean toggleRegistration = true;

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

    /**
     * Assign a new map rather than mutating this one, as an in place change to a JSON column is
     * not reliably dirty checked.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "identifier_configuration")
    private Map<ApplicableEntityType, Map<String, IdentifierConfiguration>>
        identifierConfiguration = new HashMap<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(name = "cris_national_identifier_name",
        joinColumns = @JoinColumn(name = "cris_context_information_id"),
        inverseJoinColumns = @JoinColumn(name = "mlc_id"))
    private Set<MultiLingualContent> nationalIdentifierName = new HashSet<>();
}
