package rs.teslaris.exporter.model.converter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.oaipmh.dublincore.DC;
import rs.teslaris.core.model.oaipmh.dublincore.DCMultilingualContent;
import rs.teslaris.core.model.oaipmh.dublincore.DCType;
import rs.teslaris.core.model.oaipmh.person.Affiliation;
import rs.teslaris.core.model.oaipmh.person.PersonName;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.model.common.ExportEmployment;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.common.ExportPersonName;

@Component
@Transactional
public class ExportPersonConverter extends ExportConverterBase {

    private static DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private static PersonIndexRepository personIndexRepository;

    private static InvolvementRepository involvementRepository;


    @Autowired
    public ExportPersonConverter(
        DocumentPublicationIndexRepository documentPublicationIndexRepository,
        PersonIndexRepository personIndexRepository,
        InvolvementRepository involvementRepository) {
        ExportPersonConverter.documentPublicationIndexRepository =
            documentPublicationIndexRepository;
        ExportPersonConverter.personIndexRepository = personIndexRepository;
        ExportPersonConverter.involvementRepository = involvementRepository;
    }

    public static ExportPerson toCommonExportModel(Person person, boolean computeRelations) {
        var commonExportPerson = new ExportPerson();

        setBaseFields(commonExportPerson, person);
        if (commonExportPerson.getDeleted()) {
            return commonExportPerson;
        }

        commonExportPerson.setName(
            new ExportPersonName(person.getName().getFirstname(), person.getName().getOtherName(),
                person.getName().getLastname()));
        commonExportPerson.setApvnt(person.getApvnt());
        commonExportPerson.setECrisId(person.getECrisId());
        commonExportPerson.setENaukaId(person.getENaukaId());
        commonExportPerson.setOrcid(person.getOrcid());
        commonExportPerson.setScopusAuthorId(person.getScopusAuthorId());
        commonExportPerson.setSex(person.getPersonalInfo().getSex());
        if (Objects.nonNull(person.getPersonalInfo().getContact())) {
            commonExportPerson.getElectronicAddresses()
                .add(person.getPersonalInfo().getContact().getContactEmail());
        }
        commonExportPerson.getOldIds().addAll(person.getOldIds());

        var involvements = involvementRepository.findActiveEmploymentInstitutions(person.getId());
        involvements.forEach(involvement -> commonExportPerson.getEmployments().add(
            new ExportEmployment(
                ExportOrganisationUnitConverter.toCommonExportModel(
                    involvement.getOrganisationUnit(),
                    false), involvement.getDateFrom(), involvement.getDateTo(),
                StringUtil.getStringContent(involvement.getRole(),
                    LanguageAbbreviations.ENGLISH))));

        if (computeRelations) {
            commonExportPerson.getRelatedInstitutionIds()
                .addAll(getRelatedInstitutions(person, false));
            commonExportPerson.getActivelyRelatedInstitutionIds()
                .addAll(getRelatedInstitutions(person, true));
        }
        return commonExportPerson;
    }

    private static Set<Integer> getRelatedInstitutions(Person person, boolean onlyActive) {
        var relations = new HashSet<Integer>();

        personIndexRepository.findByDatabaseId(person.getId()).ifPresent(personIndex -> {
            relations.addAll(
                personIndex.getEmploymentInstitutionsId().stream().filter(id -> id > 0).toList());

            if (!onlyActive) {
                relations.addAll(personIndex.getPastEmploymentInstitutionIds());
            }
        });

        var page = PageRequest.of(0, 500);
        Page<DocumentPublicationIndex> result;

        do {
            result = documentPublicationIndexRepository.findByAuthorIds(person.getId(), page);
            result.forEach(document -> relations.addAll(document.getOrganisationUnitIds()));
            page = page.next();
        } while (!result.isLast());

        return relations;
    }

    public static rs.teslaris.core.model.oaipmh.person.Person toOpenaireModel(
        ExportPerson exportPerson, boolean supportLegacyIdentifiers) {
        var openairePerson = new rs.teslaris.core.model.oaipmh.person.Person();

        if (supportLegacyIdentifiers && Objects.nonNull(exportPerson.getOldIds()) &&
            !exportPerson.getOldIds().isEmpty()) {
            openairePerson.setOldId("Persons/" + legacyIdentifierPrefix +
                exportPerson.getOldIds().stream().findFirst().get());
        } else {
            openairePerson.setOldId(
                "Persons/" + IdentifierUtil.identifierPrefix + exportPerson.getDatabaseId());
        }

        openairePerson.setScopusAuthorId(exportPerson.getScopusAuthorId());

        if (Objects.nonNull(exportPerson.getOrcid())) {
            openairePerson.setOrcid(
                "https://orcid.org/" + exportPerson.getOrcid().replace("https://orcid.org/", ""));
        }

        openairePerson.setPersonName(new PersonName(exportPerson.getName().getLastName(),
            exportPerson.getName().getFirstName(), null));

        openairePerson.setElectronicAddresses(new ArrayList<>());
        exportPerson.getElectronicAddresses().forEach(elAddress -> {
            openairePerson.getElectronicAddresses().add(elAddress);
        });

        if (!exportPerson.getEmployments().isEmpty()) {
            openairePerson.setAffiliation(new Affiliation(new ArrayList<>(), null));
            exportPerson.getEmployments().forEach(employment ->
                openairePerson.getAffiliation().getOrgUnits().add(
                    ExportOrganisationUnitConverter.toOpenaireModel(
                        employment.getEmploymentInstitution(), supportLegacyIdentifiers)));
        }

        return openairePerson;
    }

    public static DC toDCModel(ExportPerson exportPerson, boolean supportLegacyIdentifiers) {
        var dcPerson = new DC();
        dcPerson.getType().add(new DCType("PERSON", null, null));
        dcPerson.getSource().add(repositoryName);

        if (supportLegacyIdentifiers && Objects.nonNull(exportPerson.getOldIds()) &&
            !exportPerson.getOldIds().isEmpty()) {
            dcPerson.getIdentifier().add(
                legacyIdentifierPrefix + exportPerson.getOldIds().stream().findFirst().get());
        }

        dcPerson.getIdentifier().add(identifierPrefix + exportPerson.getDatabaseId());

        if (StringUtil.valueExists(exportPerson.getOrcid())) {
            dcPerson.getIdentifier().add(exportPerson.getOrcid());
        }

        if (StringUtil.valueExists(exportPerson.getScopusAuthorId())) {
            dcPerson.getIdentifier().add("SCOPUS:" + exportPerson.getScopusAuthorId());
        }

        if (StringUtil.valueExists(exportPerson.getENaukaId())) {
            dcPerson.getIdentifier().add("ENAUKA:" + exportPerson.getENaukaId());
        }

        if (StringUtil.valueExists(exportPerson.getECrisId())) {
            dcPerson.getIdentifier().add("ECRIS:" + exportPerson.getECrisId());
        }

        if (StringUtil.valueExists(exportPerson.getApvnt())) {
            dcPerson.getIdentifier().add("APVNT:" + exportPerson.getApvnt());
        }

        clientLanguages.forEach(lang -> {
            dcPerson.getIdentifier()
                .add(baseFrontendUrl + lang + "/persons/" +
                    exportPerson.getDatabaseId());
        });

        if (Objects.nonNull(exportPerson.getName().getMiddleName())) {
            dcPerson.getTitle()
                .add(new DCMultilingualContent(exportPerson.getName().getFirstName() + " " +
                    exportPerson.getName().getMiddleName() + " " +
                    exportPerson.getName().getLastName(), null));
        } else {
            dcPerson.getTitle().add(
                new DCMultilingualContent(exportPerson.getName().getFirstName() + " " +
                    exportPerson.getName().getLastName(), null));
        }

        addContentToList(
            exportPerson.getEmployments(),
            employment -> "oai:CRIS.UNS:Orgunits/"
                + IdentifierUtil.identifierPrefix +
                employment.getEmploymentInstitution().getDatabaseId(),
            content -> dcPerson.getRelation().add(content)
        );

        return dcPerson;
    }
}
