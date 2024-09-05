package rs.teslaris.core.model.commontypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_deduplication_blacklist")
@SQLRestriction("deleted=false")
public class DocumentDeduplicationBlacklist extends BaseEntity {

    @Column(name = "left_document_id")
    private Integer leftDocumentId;

    @Column(name = "right_document_id")
    private Integer rightDocumentId;
}
