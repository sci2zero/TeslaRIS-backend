package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_files", indexes = {
    @Index(name = "idx_doc_files_server_filename", columnList = "server_filename")
})
@SQLRestriction("deleted=false")
public class DocumentFile extends BaseEntity {

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "server_filename", nullable = false, unique = true)
    private String serverFilename;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> description = new HashSet<>();

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @Column(name = "license")
    private License license;

    @Column(name = "approve_status", nullable = false)
    private ApproveStatus approveStatus;

    @Column(name = "can_edit")
    private Boolean canEdit = true;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "latest")
    private Boolean latest = false;
}
