package rs.teslaris.core.model.document;

import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

import java.util.Set;

public class Journal {
    String eISSN;
    String printISSN;
    Set<PersonJournalContribution> contributors;
    Set<LanguageTag> languages;
    Set<MultiLingualContent> nameAbbreviation;

    Set<PersonJournalContribution> personJournalContributions
}
