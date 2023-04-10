package rs.teslaris.core.model.document;

import java.time.LocalDate;

public class PersonJournalContribution extends PersonContribution {
    JournalContributionType contributionType;
    LocalDate from;
    LocalDate to;
}
