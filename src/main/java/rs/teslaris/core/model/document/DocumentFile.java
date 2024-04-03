package rs.teslaris.core.model.document;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_files")
@Where(clause = "deleted=false")
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
}
