package rs.teslaris.core.exporter.model.converter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.exporter.model.common.ExportPerson;
import rs.teslaris.core.exporter.model.common.ExportPersonName;
import rs.teslaris.core.importer.model.oaipmh.common.DC;
import rs.teslaris.core.importer.model.oaipmh.person.Affiliation;
import rs.teslaris.core.importer.model.oaipmh.person.PersonName;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.Sex;
import rs.teslaris.core.repository.document.DocumentRepository;

@Component
@Transactional
public class ExportPersonConverter extends ExportConverterBase {

    private static DocumentRepository documentRepository;

    @Autowired
    public ExportPersonConverter(DocumentRepository documentRepository) {
        ExportPersonConverter.documentRepository = documentRepository;
    }

    public static ExportPerson toCommonExportModel(Person person) {
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
        commonExportPerson.setOldId(person.getOldId());

        person.getInvolvements().forEach(involvement -> {
            if (involvement.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                involvement.getInvolvementType().equals(InvolvementType.HIRED_BY)) {
                commonExportPerson.getEmploymentInstitutions().add(
                    ExportOrganisationUnitConverter.toCommonExportModel(
                        involvement.getOrganisationUnit()));
            }
        });

        commonExportPerson.getRelatedInstitutionIds()
            .addAll(getRelatedInstitutions(person, false));
        commonExportPerson.getActivelyRelatedInstitutionIds()
            .addAll(getRelatedInstitutions(person, true));
        return commonExportPerson;
    }

    public static Set<Integer> getRelatedInstitutions(Person person, boolean onlyActive) {
        var relations = new HashSet<Integer>();
        person.getInvolvements().forEach(involvement -> {
            if (involvement.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                involvement.getInvolvementType().equals(InvolvementType.HIRED_BY)) {
                if (onlyActive && Objects.nonNull(involvement.getDateTo()) &&
                    involvement.getDateTo().isBefore(LocalDate.now())) {
                    return;
                }
                relations.add(involvement.getOrganisationUnit().getId());
            }
        });

        documentRepository.getDocumentsForAuthorId(person.getId()).forEach(document -> {
            document.getContributors().forEach(contribution -> {
                contribution.getInstitutions().forEach(institution -> {
                    relations.add(institution.getId());
                });
            });
        });

        return relations;
    }

    public static rs.teslaris.core.importer.model.oaipmh.person.Person toOpenaireModel(
        ExportPerson exportPerson) {
        var openairePerson = new rs.teslaris.core.importer.model.oaipmh.person.Person();
        openairePerson.setOldId("TESLARIS(" + exportPerson.getDatabaseId() + ")");
        openairePerson.setScopusAuthorId(exportPerson.getScopusAuthorId());
        openairePerson.setOrcid(exportPerson.getOrcid());
        openairePerson.setPersonName(new PersonName(exportPerson.getName().getLastName(),
            exportPerson.getName().getFirstName()));

        openairePerson.setElectronicAddresses(new ArrayList<>());
        exportPerson.getElectronicAddresses().forEach(elAddress -> {
            openairePerson.getElectronicAddresses().add(elAddress);
        });

        if (Objects.nonNull(exportPerson.getSex())) {
            openairePerson.setGender(exportPerson.getSex().equals(Sex.MALE) ? "M" : "F");
        }

        if (!exportPerson.getEmploymentInstitutions().isEmpty()) {
            openairePerson.setAffiliation(new Affiliation(new ArrayList<>()));
            exportPerson.getEmploymentInstitutions().forEach(employmentInstitution -> {
                openairePerson.getAffiliation().getOrgUnits()
                    .add(ExportOrganisationUnitConverter.toOpenaireModel(employmentInstitution));
            });
        }

        return openairePerson;
    }

    public static DC toDCModel(ExportPerson exportPerson) {
        var dcPerson = new DC();
        dcPerson.getType().add("party");
        dcPerson.getSource().add(repositoryName);
        dcPerson.getIdentifier().add("TESLARIS(" + exportPerson.getDatabaseId() + ")");
        dcPerson.getIdentifier().add(exportPerson.getOrcid());
        dcPerson.getIdentifier().add(exportPerson.getScopusAuthorId());
        dcPerson.getIdentifier().add(exportPerson.getENaukaId());

        clientLanguages.forEach(lang -> {
            dcPerson.getIdentifier()
                .add(baseFrontendUrl + lang + "/persons/" +
                    exportPerson.getDatabaseId());
        });

        dcPerson.getTitle().add(exportPerson.getName().getFirstName() + " " +
            exportPerson.getName().getMiddleName() + " " + exportPerson.getName().getLastName());

        addContentToList(
            exportPerson.getEmploymentInstitutions(),
            institution -> "oai:CRIS.UNS:Orgunits/(TESLARIS)" + institution.getDatabaseId(),
            content -> dcPerson.getRelation().add(content)
        );

        return dcPerson;
    }
}
