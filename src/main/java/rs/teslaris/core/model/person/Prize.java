package rs.teslaris.core.model.person;

import java.time.LocalDate;
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
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "prizes")
public class Prize extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> title;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    Set<MultiLingualContent> description;

    @OneToMany(fetch = FetchType.LAZY)
    Set<DocumentFile> proofs;

    @Column(name = "date")
    LocalDate date;
}
