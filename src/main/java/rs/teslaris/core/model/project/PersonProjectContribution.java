package rs.teslaris.core.model.project;

import rs.teslaris.core.model.document.PersonContribution;

import java.time.LocalDate;

public class PersonProjectContribution extends PersonContribution {
    ProjectContributionType contributionType;
    LocalDate from;
    LocalDate to;
}
