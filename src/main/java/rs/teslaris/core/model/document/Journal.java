package rs.teslaris.core.model.document;

import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
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

    @Column(name = "e_issn", nullable = false, unique = true)
    String eISSN;

    @Column(name = "print_issn", nullable = false, unique = true)
    String printISSN;
    Set<PersonJournalContribution> contributors;
    Set<LanguageTag> languages;
    Set<MultiLingualContent> nameAbbreviation;

    Set<PersonJournalContribution> personJournalContributions;
}
