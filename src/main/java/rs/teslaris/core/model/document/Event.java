package rs.teslaris.core.model.document;

import rs.teslaris.core.model.commontypes.MultiLingualContent;

import java.time.LocalDate;
import java.util.Set;

public class Event {
    Set<MultiLingualContent> name;
    Set<MultiLingualContent> nameAbbreviation;
    LocalDate from;
    LocalDate to;
    Set<MultiLingualContent> state;
    Set<MultiLingualContent> place;
    Set<PersonEventContribution> contributors;

    Set<PersonEventContribution> personEventContribution;
}
