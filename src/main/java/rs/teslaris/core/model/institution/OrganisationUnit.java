package rs.teslaris.core.model.institution;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.GeoLocation;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ProfilePhotoOrLogo;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.util.deduplication.Accounted;
import rs.teslaris.core.util.deduplication.Mergeable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organisation_units", indexes = {
    @Index(name = "idx_org_unit_scopus_afid", columnList = "scopus_afid")
})
@SQLRestriction("deleted=false")
public class OrganisationUnit extends BaseEntity implements Mergeable, Accounted {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> name = new HashSet<>();

    @Column(name = "name_abbreviation", nullable = false)
    private String nameAbbreviation;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> keyword = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<ResearchArea> researchAreas = new HashSet<>();

    @Column(name = "scopus_afid")
    private String scopusAfid;

    @Column(name = "open_alex_id")
    private String openAlexId;

    @Column(name = "ror")
    private String ror;

    @Embedded
    private GeoLocation location;

    @Column(name = "approve_status", nullable = false)
    private ApproveStatus approveStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "old_ids")
    private Set<Integer> oldIds = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "merged_ids")
    private Set<Integer> mergedIds = new HashSet<>();

    @Embedded
    private Contact contact;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "uris")
    private Set<String> uris = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "accounting_ids")
    private Set<String> accountingIds = new HashSet<>();

    @Embedded
    private ProfilePhotoOrLogo logo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "allowed_thesis_types")
    private Set<String> allowedThesisTypes = new HashSet<>();

    @Column(name = "is_client_institution_cris", nullable = false)
    private Boolean isClientInstitutionCris = false;

    @Column(name = "is_client_institution_dl", nullable = false)
    private Boolean isClientInstitutionDl = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "email_configurations")
    private Map<String, EmailConfiguration> emailConfigurations = new HashMap<>();

    @Column(name = "legal_entity", nullable = false)
    private Boolean legalEntity = false;


    public EmailConfiguration getCrisConfig() {
        lazilyInitializeEmailConfiguration();

        return emailConfigurations.get("cris");
    }

    public void setCrisConfig(EmailConfiguration config) {
        lazilyInitializeEmailConfiguration();

        emailConfigurations.put("cris", config);
    }

    public EmailConfiguration getDlConfig() {
        lazilyInitializeEmailConfiguration();

        return emailConfigurations.get("digital_library");
    }

    public void setDlConfig(EmailConfiguration config) {
        lazilyInitializeEmailConfiguration();

        emailConfigurations.put("digital_library", config);
    }

    private void lazilyInitializeEmailConfiguration() {
        if (Objects.isNull(emailConfigurations)) {
            this.emailConfigurations = new HashMap<>();
        }

        emailConfigurations.putIfAbsent("cris", new EmailConfiguration());
        emailConfigurations.putIfAbsent("digital_library", new EmailConfiguration());
    }
}
