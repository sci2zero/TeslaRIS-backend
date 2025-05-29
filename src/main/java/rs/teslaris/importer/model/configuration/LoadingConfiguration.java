package rs.teslaris.importer.model.configuration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.institution.OrganisationUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loading-configurations")
public class LoadingConfiguration extends BaseEntity {

    @Column(name = "smart_loading_by_default")
    private Boolean smartLoadingByDefault;

    @Column(name = "loaded_entities_are_unmanaged")
    private Boolean loadedEntitiesAreUnmanaged;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private OrganisationUnit institution;
}
