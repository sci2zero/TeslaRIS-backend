package rs.teslaris.core.importer.model.converter.load.publication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.importer.dto.DocumentLoadDTO;
import rs.teslaris.core.importer.dto.OrganisationUnitLoadDTO;
import rs.teslaris.core.importer.dto.PersonDocumentContributionLoadDTO;
import rs.teslaris.core.importer.dto.PersonLoadDTO;
import rs.teslaris.core.importer.model.common.DocumentImport;
import rs.teslaris.core.importer.model.common.PersonDocumentContribution;
import rs.teslaris.core.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.core.importer.model.oaipmh.publication.Publication;
import rs.teslaris.core.model.document.DocumentContributionType;

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
        return person;
    }

    protected void setCommonFields(Publication record, DocumentDTO dto) {
        dto.setTitle(multilingualContentConverter.toDTO(record.getTitle()));
        dto.setSubTitle(multilingualContentConverter.toDTO(record.getSubtitle()));
        if (Objects.nonNull(record.getPublicationDate())) {
            dto.setDocumentDate(record.getPublicationDate().toString());
        }

        if (Objects.nonNull(record.getKeywords())) {
            dto.setKeywords(
                multilingualContentConverter.toDTO(String.join("\n", record.getKeywords())));
        } else {
            dto.setKeywords(new ArrayList<>());
        }

        if (Objects.nonNull(record.getUrl())) {
            dto.setUris(new HashSet<>(record.getUrl()));
        }

        dto.setDoi(record.getDoi());
        dto.setScopusId(record.getScpNumber());
        dto.setDescription(multilingualContentConverter.toDTO(record.get_abstract()));

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
        dto.setScopusId(document.getScopusId());

        setContributionInformation(document, dto);
    }

    private void setContributionInformation(Publication record, DocumentDTO dto) {
        var contributions = new ArrayList<PersonDocumentContributionDTO>();

        personContributionConverter.addContributors(record.getAuthors(),
            DocumentContributionType.AUTHOR, contributions);
        personContributionConverter.addContributors(record.getEditors(),
            DocumentContributionType.EDITOR, contributions);

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

            dto.getInstitutions().add(institution);
        });
    }
}
