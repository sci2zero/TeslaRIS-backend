package rs.teslaris.core.model.document;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;


@Getter
@Setter
@Entity
@Table(name = "journals")
public class Journal extends BaseEntity {
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> title;

    @Column(name = "e_issn", unique = true)
    String eISSN;

    @Column(name = "print_issn", unique = true)
    String printISSN;

    @OneToMany(mappedBy = "journal", fetch = FetchType.LAZY)
    Set<PersonJournalContribution> contributors;

    @OneToMany(fetch = FetchType.LAZY)
    Set<LanguageTag> languages;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> nameAbbreviation;

//    @OneToMany(fetch = FetchType.LAZY)
//    Set<PersonJournalContribution> personJournalContributions;
}
