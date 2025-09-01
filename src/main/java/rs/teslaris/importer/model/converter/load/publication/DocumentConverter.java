package rs.teslaris.importer.model.converter.load.publication;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.InSeriesDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.oaipmh.publication.BookSeries;
import rs.teslaris.core.model.oaipmh.publication.Publication;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.importer.dto.DocumentLoadDTO;
import rs.teslaris.importer.dto.OrganisationUnitLoadDTO;
import rs.teslaris.importer.dto.PersonDocumentContributionLoadDTO;
import rs.teslaris.importer.dto.PersonLoadDTO;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.PersonDocumentContribution;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class DocumentConverter {

    protected final MultilingualContentConverter multilingualContentConverter;

    protected final PublisherConverter publisherConverter;

    private final BookSeriesService bookSeriesService;

    private final JournalService journalService;

    private final PersonContributionConverter personContributionConverter;


    @NotNull
    private static PersonLoadDTO getContributorForLoader(
        PersonDocumentContribution importContribution) {
        var person = new PersonLoadDTO();
        person.setFirstName(importContribution.getPerson().getName().getFirstName());
        person.setMiddleName(importContribution.getPerson().getName().getMiddleName());
        person.setLastName(importContribution.getPerson().getName().getLastName());
        person.setECrisId(importContribution.getPerson().getECrisId());
        person.setENaukaId(importContribution.getPerson().getENaukaId());
        person.setApvnt(importContribution.getPerson().getApvnt());
        person.setOrcid(importContribution.getPerson().getOrcid());
        person.setScopusAuthorId(importContribution.getPerson().getScopusAuthorId());
        person.setOpenAlexId(importContribution.getPerson().getOpenAlexId());
        person.setWebOfScienceResearcherId(
            importContribution.getPerson().getWebOfScienceResearcherId());
        person.setImportId(importContribution.getPerson().getImportId());
        return person;
    }

    public static void addUrlsWithoutCRISUNSLandingPages(List<String> urls, DocumentDTO dto) {
        if (Objects.isNull(urls)) {
            return;
        }

        dto.setUris(new HashSet<>());
        urls.forEach(url -> {
            if (url.startsWith("https://www.cris.uns.ac.rs/record.jsf?recordId") ||
                url.startsWith("https://www.cris.uns.ac.rs/DownloadFileServlet")) {
                return;
            }

            dto.getUris().add(url);
        });
    }

    protected static String deduceLanguageTagValue(Publication record) {
        var languageTagValue = record.getLanguage().trim().toUpperCase();
        if (languageTagValue.isEmpty()) {
            languageTagValue = LanguageAbbreviations.ENGLISH;
        }

        // Common language tag mistakes
        if (languageTagValue.equals("GE")) {
            languageTagValue = LanguageAbbreviations.GERMAN;
        } else if (languageTagValue.equals("SP")) {
            languageTagValue = LanguageAbbreviations.SPANISH;
        } else if (languageTagValue.equals("RS")) {
            languageTagValue = LanguageAbbreviations.SERBIAN;
        }

        return languageTagValue;
    }

    protected void setCommonFields(Publication record, DocumentDTO dto) {
        dto.setTitle(multilingualContentConverter.toDTO(record.getTitle()));
        dto.setSubTitle(multilingualContentConverter.toDTO(record.getSubtitle()));
        if (Objects.nonNull(record.getPublicationDate())) {
            dto.setDocumentDate(String.valueOf(
                record.getPublicationDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    .getYear()));
        }

        if (Objects.nonNull(record.getKeywords())) {
            dto.setKeywords(multilingualContentConverter.toDTO(
                OAIPMHParseUtility.groupParsedMultilingualKeywords(record.getKeywords())));
        } else {
            dto.setKeywords(new ArrayList<>());
        }

        addUrlsWithoutCRISUNSLandingPages(record.getUrl(), dto);

        dto.setDoi(Objects.nonNull(record.getDoi()) ? record.getDoi().replace("|", "") : null);
        dto.setScopusId(record.getScpNumber());

        if (Objects.nonNull(record.get_abstract()) && !record.get_abstract().isEmpty()) {
            record.get_abstract()
                .forEach(abs -> abs.setValue(abs.getValue().replace("<br />", " ")));
            dto.setDescription(multilingualContentConverter.toDTO(record.get_abstract()));
        } else {
            dto.setDescription(new ArrayList<>());
        }

        setContributionInformation(record, dto);

        if (Objects.nonNull(record.getNote()) && !record.getNote().isEmpty()) {
            dto.setNote(record.getNote().getFirst().getValue());
        }
    }

    protected void setCommonThesisFields(Publication record, ThesisDTO dto,
                                         LanguageTagService languageTagService,
                                         OrganisationUnitService organisationUnitService) {
        if (Objects.nonNull(record.getLanguage()) && record.getLanguage().equals("sr-Cyrl")) {
            record.setLanguage("SR-CYR");
        }
        dto.setWritingLanguageTagId(
            languageTagService.findLanguageTagByValue(record.getLanguage()).getId());

        if (Objects.isNull(record.getInstitutions()) || record.getInstitutions().isEmpty()) {
            log.error("Thesis with ID {} has no specified institutions. Skipping.", dto.getOldId());
            throw new NotFoundException("Thesis OU not specified.");
        }

        var publisher = record.getInstitutions().getFirst();
        if (Objects.nonNull(publisher)) {
            if (Objects.nonNull(publisher.getOrgUnit()) &&
                Objects.nonNull(publisher.getOrgUnit().getOldId())) {
                var organisationUnit = organisationUnitService.findOrganisationUnitByOldId(
                    OAIPMHParseUtility.parseBISISID(publisher.getOrgUnit().getOldId()));

                if (Objects.isNull(organisationUnit)) {
                    log.error(
                        "Unable to migrate thesis with ID {}. Because OU with ID {} does not exist.",
                        record.getOldId(), publisher.getOrgUnit().getOldId());
                    throw new NotFoundException("Thesis OU not found.");
                }

                dto.setOrganisationUnitId(organisationUnit.getId());
            } else {
                dto.setExternalOrganisationUnitName(
                    multilingualContentConverter.toDTO(publisher.getDisplayName()));
            }
        }

        dto.setAlternateTitle(multilingualContentConverter.toDTO(record.getAlternativeTitle()));
        dto.setExtendedAbstract(multilingualContentConverter.toDTO(record.getExtendedAbstract()));

        dto.setNumberOfPages(record.getNumberOfPages());
        dto.setNumberOfChapters(record.getNumberOfChapters());
        dto.setNumberOfReferences(record.getNumberOfReferences());
        dto.setNumberOfTables(record.getNumberOfTables());
        dto.setNumberOfIllustrations(record.getNumberOfPictures());
        dto.setNumberOfGraphs(record.getNumberOfGraphs());
        dto.setNumberOfAppendices(record.getNumberOfAppendixes());
        dto.setEisbn(record.getIsbn());
        dto.setUdc(record.getUdc());

        dto.setScientificArea(multilingualContentConverter.toDTO(record.getResearchArea()));
        dto.setTypeOfTitle(multilingualContentConverter.toDTO(record.getLevelOfEducation()));
        dto.setPlaceOfKeep(multilingualContentConverter.toDTO(record.getHoldingData()));

        if (Objects.nonNull(record.getAcceptedOnDate())) {
            dto.setTopicAcceptanceDate(
                LocalDate.ofInstant(record.getAcceptedOnDate().toInstant(),
                    ZoneId.systemDefault()));
        }

        if (Objects.nonNull(record.getDefendedOnDate())) {
            dto.setThesisDefenceDate(
                LocalDate.ofInstant(record.getDefendedOnDate().toInstant(),
                    ZoneId.systemDefault()));
        }

        if (Objects.nonNull(record.getPublicReviewStartDate())) {
            dto.setPublicReviewStartDate(
                LocalDate.ofInstant(record.getPublicReviewStartDate().toInstant(),
                    ZoneId.systemDefault()));
        }

        if (Objects.nonNull(record.getPublisher())) {
            publisherConverter.setPublisherInformation(record.getPublisher(), dto);
        }
    }

    protected void setCommonFields(DocumentImport document, DocumentLoadDTO dto) {
        dto.setTitle(multilingualContentConverter.toLoaderDTO(document.getTitle()));
        dto.setSubTitle(multilingualContentConverter.toLoaderDTO(document.getSubtitle()));
        dto.setKeywords(multilingualContentConverter.toLoaderDTO(document.getKeywords()));
        dto.setDescription(multilingualContentConverter.toLoaderDTO(document.getDescription()));

        dto.setDocumentDate(document.getDocumentDate());
        dto.setUris(new HashSet<>(document.getUris()));

        dto.setDoi(document.getDoi());
        dto.setOpenAlexId(document.getOpenAlexId());
        dto.setScopusId(document.getScopusId());
        dto.setWebOfScienceId(document.getWebOfScienceId());
        dto.setInternalIdentifiers(document.getInternalIdentifiers().stream().toList());

        setContributionInformation(document, dto);
    }

    private void setContributionInformation(Publication record, DocumentDTO dto) {
        var contributions = new ArrayList<PersonDocumentContributionDTO>();

        personContributionConverter.addContributors(record.getAuthors(),
            DocumentContributionType.AUTHOR, contributions);
        personContributionConverter.addContributors(record.getEditors(),
            DocumentContributionType.EDITOR, contributions);
        personContributionConverter.addContributors(record.getAdvisors(),
            DocumentContributionType.ADVISOR, contributions);
        personContributionConverter.addContributors(record.getBoardMembers(),
            DocumentContributionType.BOARD_MEMBER, contributions);

        dto.setContributions(contributions);
    }

    private void setContributionInformation(DocumentImport document, DocumentLoadDTO dto) {
        document.getContributions().forEach(importContribution -> {
            var contribution = new PersonDocumentContributionLoadDTO();
            contribution.setOrderNumber(importContribution.getOrderNumber());
            contribution.setContributionType(importContribution.getContributionType());
            contribution.setIsMainContributor(importContribution.getIsMainContributor());
            contribution.setIsCorrespondingContributor(
                importContribution.getIsCorrespondingContributor());

            contribution.setContributionDescription(multilingualContentConverter.toLoaderDTO(
                importContribution.getContributionDescription()));

            var person = getContributorForLoader(importContribution);
            contribution.setPerson(person);

            setInstitutions(importContribution, contribution);

            dto.getContributions().add(contribution);
        });
    }

    private void setInstitutions(PersonDocumentContribution importContribution,
                                 PersonDocumentContributionLoadDTO dto) {
        importContribution.getInstitutions().forEach(importInstitution -> {
            var institution = new OrganisationUnitLoadDTO();
            institution.setName(
                multilingualContentConverter.toLoaderDTO(importInstitution.getName()));
            institution.setNameAbbreviation(importInstitution.getNameAbbreviation());
            institution.setScopusAfid(importInstitution.getScopusAfid());
            institution.setOpenAlexId(importInstitution.getOpenAlexId());
            institution.setImportId(importInstitution.getImportId());

            dto.getInstitutions().add(institution);
        });
    }

    protected void setBookSeriesInformation(BookSeries bookSeries, InSeriesDTO dto) {
        if (Objects.nonNull(bookSeries.getIssn())) {
            var potentialMatch = bookSeriesService.readBookSeriesByIssn(bookSeries.getIssn(),
                bookSeries.getIssn());
            if (Objects.nonNull(potentialMatch)) {
                dto.setPublicationSeriesId(potentialMatch.getDatabaseId());
                return;
            } else {
                var potentialJournalMatch =
                    journalService.readJournalByIssn(bookSeries.getIssn(), bookSeries.getIssn());
                if (Objects.nonNull(potentialJournalMatch)) {
                    dto.setPublicationSeriesId(potentialJournalMatch.getDatabaseId());
                    return;
                }
            }
        }

        if (Objects.isNull(dto.getPublicationSeriesId())) {
            var name = bookSeries.getTitle();
            var potentialMatches = bookSeriesService.searchBookSeries(
                Arrays.stream(name.split(" ")).filter(n -> !n.isBlank()).toList(),
                PageRequest.of(0, 1));
            if (potentialMatches.hasContent()) {
                var match = potentialMatches.getContent().getFirst();
                if (match.getTitleSr().equals(name) || match.getTitleOther().equals(name)) {
                    dto.setPublicationSeriesId(match.getDatabaseId());
                    return;
                }
            }
        }

        if (Objects.isNull(dto.getPublicationSeriesId())) {
            var bookSeriesDTO = new BookSeriesDTO();
            bookSeriesDTO.setTitle(multilingualContentConverter.toDTO(bookSeries.getTitle()));
            bookSeriesDTO.setContributions(new ArrayList<>());
            bookSeriesDTO.setLanguageTagIds(new ArrayList<>());
            bookSeriesDTO.setNameAbbreviation(new ArrayList<>());
            bookSeriesDTO.setUris(new HashSet<>());

            bookSeriesDTO.setEissn(bookSeries.getIssn());

            dto.setPublicationSeriesId(
                bookSeriesService.createBookSeries(bookSeriesDTO, true).getId());
        }
    }
}
