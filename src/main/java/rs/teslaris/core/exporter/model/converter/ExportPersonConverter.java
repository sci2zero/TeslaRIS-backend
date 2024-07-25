package rs.teslaris.core.exporter.model.converter;

import rs.teslaris.core.exporter.model.common.ExportPerson;
import rs.teslaris.core.exporter.model.common.ExportPersonName;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;

public class ExportPersonConverter {

    public static ExportPerson toCommonExportModel(Person person) {
        var commonExportPerson = new ExportPerson();
        commonExportPerson.setDatabaseId(person.getId());
        commonExportPerson.setLastUpdated(person.getLastModification());

        if (person.getDeleted()) {
            commonExportPerson.setDeleted(true);
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
        commonExportPerson.getElectronicAddresses()
            .add(person.getPersonalInfo().getContact().getContactEmail());

        person.getInvolvements().forEach(involvement -> {
            if (involvement.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                involvement.getInvolvementType().equals(InvolvementType.HIRED_BY)) {
                commonExportPerson.getEmploymentInstitutions().add(
                    ExportOrganisationUnitConverter.toCommonExportModel(
                        involvement.getOrganisationUnit()));
            }
        });

        return commonExportPerson;
    }
}
