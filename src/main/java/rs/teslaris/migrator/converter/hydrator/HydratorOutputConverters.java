package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.JournalPublicationType;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;

/**
 * Converters for the curriculum output kinds this PoC supports.
 * <p>
 * Contributions are created name-only except for the curriculum owner, whose TeslaRIS id is known.
 * Co-authors appear in their own curricula and are linked there.
 */
@Component
@RequiredArgsConstructor
public class HydratorOutputConverters {

    private static final String DOI_CODE = "DOI";

    private final HydratorConversionUtil conversionUtil;

    private final HydratorJournalResolver journalResolver;


    public JournalPublicationDTO toJournalPublication(HydratorCVModel.Curriculum record,
                                                      HydratorCVModel.Output output) {
        var article = output.journalArticle();

        if (Objects.isNull(article) || isBlank(article.articleTitle())) {
            return null;
        }

        var language = languageOf(record);
        var dto = new JournalPublicationDTO();

        applyCommonFields(dto, record, article.articleTitle(), language,
            article.publicationDate(), article.url(), article.identifiers(), article.authors());

        dto.setJournalPublicationType(JournalPublicationType.RESEARCH_ARTICLE);
        dto.setVolume(article.volume());
        dto.setIssue(article.issue());
        dto.setStartPage(article.pageFrom());
        dto.setEndPage(article.pageTo());
        dto.setJournalId(journalResolver.resolveOrCreate(article.journal(), language));

        return dto;
    }

    public ThesisDTO toThesis(HydratorCVModel.Curriculum record, HydratorCVModel.Output output) {
        var dissertation = output.dissertation();

        if (Objects.isNull(dissertation) || isBlank(dissertation.title())) {
            return null;
        }

        var language = languageOf(record);
        var dto = new ThesisDTO();

        applyCommonFields(dto, record, dissertation.title(), language,
            dissertation.completionDate(), dissertation.url(), dissertation.identifiers(),
            dissertation.authors());

        dto.setThesisType(thesisType(dissertation.degreeType()));

        return dto;
    }

    private void applyCommonFields(DocumentDTO dto, HydratorCVModel.Curriculum record, String title,
                                   String language, HydratorCVModel.DateInfo date, String url,
                                   HydratorCVModel.OutputIdentifiers identifiers,
                                   HydratorCVModel.OutputAuthors authors) {
        dto.setTitle(conversionUtil.multilingualContent(title, language));
        dto.setSubTitle(List.of());
        dto.setDescription(List.of());
        dto.setKeywords(List.of());
        dto.setUris(new HashSet<>());
        dto.setDocumentDate(conversionUtil.flexibleDate(date));
        dto.setDoi(identifierValue(identifiers, DOI_CODE));
        dto.setContributions(contributions(record, authors));

        if (!isBlank(url)) {
            dto.getUris().add(url.trim());
        }
    }

    private List<PersonDocumentContributionDTO> contributions(HydratorCVModel.Curriculum record,
                                                              HydratorCVModel.OutputAuthors authors) {
        var contributions = new ArrayList<PersonDocumentContributionDTO>();

        if (Objects.isNull(authors) || Objects.isNull(authors.author())) {
            return contributions;
        }

        var orderNumber = 1;

        for (var author : authors.author()) {
            if (isBlank(author.name()) && isBlank(author.surname())) {
                continue;
            }

            var contribution = new PersonDocumentContributionDTO();
            contribution.setContributionType(DocumentContributionType.AUTHOR);
            contribution.setIsMainContributor(orderNumber == 1);
            contribution.setIsCorrespondingContributor(false);
            contribution.setIsBoardPresident(false);
            contribution.setOrderNumber(orderNumber++);
            contribution.setContributionDescription(List.of());
            contribution.setDisplayAffiliationStatement(List.of());
            contribution.setInstitutionIds(List.of());

            var personName = new PersonNameDTO();
            personName.setFirstname(Objects.toString(author.name(), "").trim());
            personName.setLastname(Objects.toString(author.surname(), "").trim());
            contribution.setPersonName(personName);

            contributions.add(contribution);
        }

        return contributions;
    }

    private String identifierValue(HydratorCVModel.OutputIdentifiers identifiers, String code) {
        if (Objects.isNull(identifiers) || Objects.isNull(identifiers.identifier())) {
            return null;
        }

        return identifiers.identifier().stream()
            .filter(identifier -> Objects.nonNull(identifier.identifierType()))
            .filter(identifier -> code.equalsIgnoreCase(identifier.identifierType().code()) ||
                code.equalsIgnoreCase(identifier.identifierType().value()))
            .map(HydratorCVModel.OutputIdentifier::identifier)
            .filter(value -> !isBlank(value))
            .findFirst()
            .orElse(null);
    }

    private ThesisType thesisType(HydratorCVModel.DegreeType degreeType) {
        if (Objects.isNull(degreeType)) {
            return ThesisType.PHD;
        }

        var value = Objects.toString(degreeType.value(), "").toLowerCase();

        if (value.contains("master")) {
            return ThesisType.MASTER;
        }

        if (value.contains("magist")) {
            return ThesisType.MR;
        }

        return ThesisType.PHD;
    }

    private String languageOf(HydratorCVModel.Curriculum record) {
        return Objects.isNull(record.curriculum()) ? null : record.curriculum().language();
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
