package rs.teslaris.thesislibrary.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.OrganisationUnit;

@Getter
@Setter
@Entity
@Table(name = "registry_book_entries")
@SQLRestriction("deleted=false")
public class RegistryBookEntry extends BaseEntity {

    @Embedded
    private DissertationInformation dissertationInformation;

    @Embedded
    private RegistryBookPersonalInformation personalInformation;

    @Embedded
    private RegistryBookContactInformation contactInformation;

    @Embedded
    private PreviousTitleInformation previousTitleInformation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;
}
