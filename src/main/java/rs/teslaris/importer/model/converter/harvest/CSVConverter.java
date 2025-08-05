package rs.teslaris.importer.model.converter.harvest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.util.FunctionalUtil;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.Event;
import rs.teslaris.importer.model.common.MultilingualContent;
import rs.teslaris.importer.model.common.OrganisationUnit;
import rs.teslaris.importer.model.common.Person;
import rs.teslaris.importer.model.common.PersonDocumentContribution;
import rs.teslaris.importer.model.common.PersonName;

public class CSVConverter {

    private static final Pattern orcidRegexPattern =
        Pattern.compile("^\\d{4}-\\d{4}-\\d{4}-[\\dX]{4}$", Pattern.CASE_INSENSITIVE);

    private static final Pattern scopusAuthorIdPattern =
        Pattern.compile("^\\d+$", Pattern.CASE_INSENSITIVE);

    /**
     * Converts a row from a CSV containing publication metadata into a standard internal format.
     *
     * <p>This method assumes the input CSV has columns in a fixed order, where each column maps to
     * specific metadata about a publication. Below is a description of each column index and the type
     * of data it holds, with anonymized examples:</p>
     *
     * <ul>
     *   <li><b>[0]</b> *Publication Type - "Article", "Conference paper"</li>
     *   <li><b>[1]</b> *Authors - Author name variants that they used in publication </li>
     *   <li><b>[2]</b> Author Full Names with Identifier - "Smith, John (12345678900); Doe, Jane (09876543210)"</li>
     *   <li><b>[3]</b> Author Full Names with Affiliations - "Smith J., University of Something, City; Jane D., University, City"</li>
     *   <li><b>[4]</b> *Title - "Integration of a system with an OAI-PMH compatible repository"</li>
     *   <li><b>[5]</b> *Year - "2012"</li>
     *   <li><b>[6]</b> Volume - "56"</li>
     *   <li><b>[7]</b> Issue - "2"</li>
     *   <li><b>[8]</b> Start Page - "104"</li>
     *   <li><b>[9]</b> End Page - "112"</li>
     *   <li><b>[10]</b> Pages - if you want to supply page range, free string format</li>
     *   <li><b>[11]</b> DOI</li>
     *   <li><b>[12]</b> Link / URL</li>
     *   <li><b>[13]</b> Abstract</li>
     *   <li><b>[14]</b> Author supplied keywords, separated by "; "</li>
     *   <li><b>[15]</b> *Conference name</li>
     *   <li><b>[16]</b> *Journal Name</li>
     *   <li><b>[17]</b> ISSN</li>
     *   <li><b>[18]</b> ISBN</li>
     *   <li><b>[19]</b> ConfID</li>
     *   <li><b>[20]</b> *Electronic ID</li>
     * </ul>
     */
    public static Optional<DocumentImport> toCommonImportModel(String[] record) {
        if (isMissingRequiredFields(record)) {
            return Optional.empty();
        }

        var document = new DocumentImport();
        populateBasicMetadata(document, record);

        if (!populatePublicationTypeAndEvent(document, record)) {
            return Optional.empty();
        }

        populateAuthors(document, record);
        populatePagination(document, record);
        populateIdentifiersAndDescriptions(document, record);

        return Optional.of(document);
    }

    private static boolean isMissingRequiredFields(String[] record) {
        return record[4].isBlank() || record[5].isBlank() || record[20].isBlank();
    }

    private static void populateBasicMetadata(DocumentImport document, String[] record) {
        document.getTitle().add(new MultilingualContent("EN", record[4], 1));
        document.setDocumentDate(record[5]);
        document.setIdentifier(record[20]);
        document.setVolume(record[6]);
        document.setIssue(record[7]);
    }

    private static boolean populatePublicationTypeAndEvent(DocumentImport document,
                                                           String[] record) {
        String type = record[0];

        if ("Article".equals(type) && !record[16].isBlank()) {
            document.setPublicationType(DocumentPublicationType.JOURNAL_PUBLICATION);
            document.getPublishedIn().add(new MultilingualContent("EN", record[16], 1));
        } else if ("Conference paper".equals(type) && !record[15].isBlank()) {
            document.setPublicationType(DocumentPublicationType.PROCEEDINGS_PUBLICATION);
            document.getPublishedIn()
                .add(new MultilingualContent("EN", "Proceedings of " + record[15], 1));

            var event = new Event();
            event.getName().add(new MultilingualContent("EN", record[15], 1));
            event.setConfId(record[19]);

            int year = Integer.parseInt(record[5]);
            event.setDateFrom(LocalDate.of(year, 1, 1));
            event.setDateTo(LocalDate.of(year, 12, 31));

            document.setEvent(event);
        } else {
            return false;
        }

        return true;
    }

    private static void populateAuthors(DocumentImport document, String[] record) {
        var identifiers = record[2].split("; ");
        var affiliations = record[3].split("; ");
        var names = record[1].split("; ");

        FunctionalUtil.forEachWithCounter(Arrays.asList(names), (i, authorName) -> {
            var contribution = new PersonDocumentContribution();
            contribution.setOrderNumber(i + 1);
            contribution.setContributionType(DocumentContributionType.AUTHOR);

            var person = createPerson(authorName, i < identifiers.length ? identifiers[i] : null);
            contribution.setPerson(person);

            if (i < affiliations.length) {
                var institution = createInstitution(i, affiliations[i]);
                contribution.getInstitutions().add(institution);
            }

            document.getContributions().add(contribution);
        });
    }

    private static Person createPerson(String name, String identifierEntry) {
        var parts = name.split(" ");
        var person = new Person();
        person.setName(new PersonName(parts[1], "", parts[0]));

        if (identifierEntry != null && identifierEntry.contains("(")) {
            String identifier = identifierEntry.split("\\(")[1].replace(")", "");
            person.setImportId(identifier);

            if (scopusAuthorIdPattern.matcher(identifier).matches()) {
                person.setScopusAuthorId(identifier);
            } else if (orcidRegexPattern.matcher(identifier).matches()) {
                person.setOrcid(identifier);
            }
        }

        return person;
    }

    private static OrganisationUnit createInstitution(int index, String affiliation) {
        var institution = new OrganisationUnit();
        institution.setImportId(String.valueOf(index));
        var name = affiliation.split(", ", 2)[1];
        institution.getName().add(new MultilingualContent("EN", name, 1));
        return institution;
    }

    private static void populatePagination(DocumentImport document, String[] record) {
        if (record[10].isBlank()) {
            document.setStartPage(record[8]);
            document.setEndPage(record[9]);

            if (!record[8].isBlank() && !record[9].isBlank()) {
                document.setNumberOfPages(
                    Integer.parseInt(record[9]) - Integer.parseInt(record[8]));
            }
        } else {
            var pageRange = record[10];
            if (pageRange.contains("-")) {
                var pages = pageRange.split("-");
                document.setStartPage(pages[0]);
                document.setEndPage(pages[1]);
                document.setNumberOfPages(Integer.parseInt(pages[1]) - Integer.parseInt(pages[0]));
            } else {
                document.setStartPage(pageRange);
                document.setEndPage("");
            }
        }
    }

    private static void populateIdentifiersAndDescriptions(DocumentImport document,
                                                           String[] record) {
        document.setDoi(record[11]);

        if (!record[12].isBlank()) {
            document.getUris().add(record[12]);
        }

        if (!record[17].isBlank()) {
            document.setEIssn(formatIssn(record[17]));
        }

        document.setIsbn(record[18]);

        document.getDescription().add(new MultilingualContent("EN", record[13], 1));
        document.getKeywords()
            .add(new MultilingualContent("EN", record[14].replace("; ", "\n"), 1));
    }

    private static String formatIssn(String raw) {
        return raw.contains("-") ? raw : raw.substring(0, 4) + "-" + raw.substring(4);
    }
}
