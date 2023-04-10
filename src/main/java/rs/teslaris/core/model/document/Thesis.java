package rs.teslaris.core.model.document;

import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.institution.OrganisationUnit;

import java.util.Set;

public class Thesis extends Document {
    OrganisationUnit organisationUnit;
    ThesisCategory category;
    Integer numberOfPages;
    Set<LanguageTag> languages;
    ResearchArea researchArea;
    Publisher publisher;
}
