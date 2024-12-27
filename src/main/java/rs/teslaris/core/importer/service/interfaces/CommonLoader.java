package rs.teslaris.core.importer.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;

@Service
public interface CommonLoader {

    <R> R loadRecordsWizard(Integer userId);

    <R> R loadSkippedRecordsWizard(Integer userId);

    void skipRecord(Integer userId);

    void markRecordAsLoaded(Integer userId);

    Integer countRemainingDocumentsForLoading(Integer userId);

    OrganisationUnitDTO createInstitution(String scopusAfid, Integer userId);

    PersonResponseDTO createPerson(String scopusAuthorId, Integer userId);

    PublicationSeriesDTO createJournal(String eIssn, String printIssn, Integer userId);

    ProceedingsDTO createProceedings(Integer userId);
}
