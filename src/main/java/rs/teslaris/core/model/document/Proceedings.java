package rs.teslaris.core.model.document;

import rs.teslaris.core.model.commontypes.LanguageTag;

import java.util.Set;

public class Proceedings extends Document {
    String eISBN;
    String printISBN;
    Integer numberOfPages;
    String editionTitle;
    Integer editionNumber;
    String editionISSN;
    Set<LanguageTag> languages;
    Journal journal;
    String journalVolume;
    String journalIssue;
    Conference Conference;
    Publisher publisher;
}
