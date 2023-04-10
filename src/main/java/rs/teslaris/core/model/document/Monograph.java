package rs.teslaris.core.model.document;

import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.ResearchArea;

import java.util.Set;

public class Monograph extends Document {
    String printISBN;
    String eISBN;
    Integer numberOfPages;
    String editionTitle;
    Integer editionNumber;
    String editionISSN;
    Set<LanguageTag> languages;
    ResearchArea researchArea;
}
