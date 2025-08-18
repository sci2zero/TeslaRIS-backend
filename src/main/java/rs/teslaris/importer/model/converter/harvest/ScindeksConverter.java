package rs.teslaris.importer.model.converter.harvest;

import java.util.ArrayList;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.oaipmh.dublincore.DC;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.MultilingualContent;
import rs.teslaris.importer.model.common.Person;
import rs.teslaris.importer.model.common.PersonDocumentContribution;
import rs.teslaris.importer.model.common.PersonName;

@Slf4j
public class ScindeksConverter {

    public static Optional<DocumentImport> toCommonImportModel(DC record) {
        var document = new DocumentImport();
        document.setIdentifier("SCINDEKS:" + record.getIdentifier().getFirst().split("=")[1]);
        document.setDocumentDate(record.getDate().getFirst());
        document.getInternalIdentifiers().add(document.getIdentifier());

        document.getTitle().add(new MultilingualContent("EN", record.getTitle().getFirst(), 1));
        document.getDescription()
            .add(new MultilingualContent("EN", record.getDescription().getFirst(), 1));

        if (record.getType().stream()
            .anyMatch(source -> source.equals("info:eu-repo/semantics/article"))) {
            document.setPublicationType(DocumentPublicationType.JOURNAL_PUBLICATION);

            var journalName =
                record.getSource().stream().filter(name -> !name.startsWith("ISSN:")).findFirst();
            if (journalName.isEmpty()) {
                return Optional.empty();
            }

            document.getPublishedIn()
                .add(new MultilingualContent("EN", journalName.get(), 1));

            record.getSource().stream().filter(name -> name.startsWith("ISSN:")).findFirst()
                .ifPresent(issn -> {
                    document.setEIssn(issn.split(":")[1].trim());
                });
        } else {
            log.error("UNKNOWN TYPE:");
            record.getType().forEach(System.out::println);
        }

        FunctionalUtil.forEachWithCounter(record.getCreator(), (i, authorship) -> {
            var contribution = new PersonDocumentContribution();
            contribution.setOrderNumber(i + 1);
            contribution.setContributionType(DocumentContributionType.AUTHOR);
            contribution.setIsCorrespondingContributor(false);

            var person = new Person();
            var authorFullName = authorship;
            var authorIdentifier = "";
            if (authorship.contains(";")) {
                var authorshipParts = authorship.split(";");
                authorFullName = authorshipParts[0].trim();
                authorIdentifier = authorshipParts[1].trim();
            }

            var personNameParts = authorFullName.split(", ");
            person.setName(new PersonName(personNameParts[1], "", personNameParts[0]));

            if (!authorIdentifier.isBlank()) {
                if (authorIdentifier.startsWith("id_orcid")) {
                    person.setImportId(authorIdentifier.split(" ")[1]);
                    person.setOrcid(person.getImportId());
                } else {
                    log.warn("Encountered unknown Scindeks author identifier: {}",
                        authorIdentifier);
                }
            }

            contribution.setPerson(person);
            document.getContributions().add(contribution);
        });

        var keywords = new ArrayList<>(record.getSubject());
        document.getKeywords().add(new MultilingualContent("EN", Strings.join(keywords, '\n'), 1));

        return Optional.of(document);
    }
}
