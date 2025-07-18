package rs.teslaris.importer.model.converter.load.publication;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.oaipmh.publication.Publication;
import rs.teslaris.importer.dto.DocumentLoadDTO;
import rs.teslaris.importer.dto.OrganisationUnitLoadDTO;
import rs.teslaris.importer.dto.PersonDocumentContributionLoadDTO;
import rs.teslaris.importer.dto.PersonLoadDTO;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.PersonDocumentContribution;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Component
@RequiredArgsConstructor
public abstract class DocumentConverter {

    protected final MultilingualContentConverter multilingualContentConverter;

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
}
