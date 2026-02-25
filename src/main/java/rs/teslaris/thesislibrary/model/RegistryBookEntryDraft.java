package rs.teslaris.thesislibrary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.document.Thesis;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "registry_book_entry_drafts")
public class RegistryBookEntryDraft extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thesis_id", nullable = false)
    private Thesis thesis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "draft_data", columnDefinition = "jsonb")
    private String draftData;
}
